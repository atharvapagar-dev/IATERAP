package com.adtech.iaterap.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.adtech.iaterap.backend.dto.AiSuggestionRequest;
import com.adtech.iaterap.backend.dto.AiSuggestionResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * AI-249: starts the real FastAPI service and validates the Spring HTTP client
 * against its live /suggest contract. No HTTP stub or mock server is used.
 */
@SpringBootTest
@ActiveProfiles("test")
class SpringFastApiSuggestionIntegrationTest {

    private static Process fastApi;
    private static int port;
    private static Path fastApiLog;

    @Autowired
    private AiAdvisoryClient client;

    @DynamicPropertySource
    static void fastApiProperties(DynamicPropertyRegistry registry) {
        startFastApi();
        registry.add("ai.advisory.base-url", () -> "http://127.0.0.1:" + port);
        registry.add("ai.advisory.connect-timeout-ms", () -> 3000);
        registry.add("ai.advisory.read-timeout-ms", () -> 5000);
    }

    @AfterAll
    static void stopFastApi() {
        if (fastApi != null && fastApi.isAlive()) {
            fastApi.destroy();
            try {
                if (!fastApi.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    fastApi.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                fastApi.destroyForcibly();
            }
        }
    }

    @Test
    void springBackendCallsFastApiSuggestAndReceivesStructuredResponse() {
        AiSuggestionResponse response = client.suggest(
                new AiSuggestionRequest(42L, 7L, "Check the expected bus-recovery behaviour."));

        assertFalse(response.suggestion().isBlank());
        assertEquals(0D, response.confidence());
        assertEquals("stub", response.source());
        assertNotNull(response.generatedAt());
    }

    private static synchronized void startFastApi() {
        if (fastApi != null && fastApi.isAlive()) {
            return;
        }
        port = availablePort();
        Path serviceDirectory = locateAiService();
        String python = System.getProperty("os.name").toLowerCase().contains("win") ? "py" : "python3";
        try {
            fastApi = new ProcessBuilder(python, "-m", "uvicorn", "app.main:app",
                    "--host", "127.0.0.1", "--port", String.valueOf(port))
                    .directory(serviceDirectory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.to(fastApiLog().toFile()))
                    .start();
            waitForHealth();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start FastAPI service for AI-249 integration test", exception);
        } catch (RuntimeException exception) {
            stopFastApi();
            throw exception;
        }
    }

    private static void waitForHealth() {
        // Cold Python environments can take longer while importing scientific
        // dependencies; CI still fails deterministically rather than racing it.
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            if (fastApi == null || !fastApi.isAlive()) {
                throw new IllegalStateException("FastAPI service exited before becoming healthy: " + fastApiOutput());
            }
            try {
                HttpURLConnection connection = (HttpURLConnection) URI
                        .create("http://127.0.0.1:" + port + "/health").toURL().openConnection();
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);
                if (connection.getResponseCode() == 200) return;
            } catch (IOException ignored) {
                // Uvicorn has not yet bound the port.
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for FastAPI service", exception);
            }
        }
        throw new IllegalStateException("FastAPI service did not become healthy within 60 seconds: " + fastApiOutput());
    }

    private static int availablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to reserve a local port for FastAPI", exception);
        }
    }

    private static Path locateAiService() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("ai-service");
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate the ai-service directory for AI-249");
    }

    private static Path fastApiLog() {
        if (fastApiLog == null) {
            try {
                fastApiLog = Files.createTempFile("iaterap-fastapi-ai249-", ".log");
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to create FastAPI integration log", exception);
            }
        }
        return fastApiLog;
    }

    private static String fastApiOutput() {
        try {
            return fastApiLog == null ? "<no process output>" : Files.readString(fastApiLog);
        } catch (IOException exception) {
            return "<unable to read process output: " + exception.getMessage() + ">";
        }
    }
}
