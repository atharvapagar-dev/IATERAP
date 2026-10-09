package com.adtech.iaterap.backend.ai;

import com.adtech.iaterap.backend.dto.AiSuggestionRequest;
import com.adtech.iaterap.backend.dto.AiSuggestionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** HTTP-only boundary to the independently deployed FastAPI advisory service. */
@Service
public class AiAdvisoryClient {

    private final RestClient client;

    public AiAdvisoryClient(
            @Value("${ai.advisory.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${ai.advisory.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${ai.advisory.read-timeout-ms:5000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public AiSuggestionResponse suggest(AiSuggestionRequest request) {
        try {
            AiSuggestionResponse response = client.post().uri("/suggest")
                    .body(request).retrieve().body(AiSuggestionResponse.class);
            if (response == null) {
                throw new AiAdvisoryUnavailableException("AI advisory service returned an empty response", null);
            }
            return response;
        } catch (AiAdvisoryUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiAdvisoryUnavailableException(
                    "AI advisory service is unavailable or returned an invalid response", exception);
        }
    }
}
