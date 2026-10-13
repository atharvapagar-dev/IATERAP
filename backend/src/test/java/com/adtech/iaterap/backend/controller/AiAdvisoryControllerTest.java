package com.adtech.iaterap.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adtech.iaterap.backend.ai.AiAdvisoryClient;
import com.adtech.iaterap.backend.ai.AiAdvisoryUnavailableException;
import com.adtech.iaterap.backend.dto.AiSuggestionResponse;
import com.adtech.iaterap.backend.security.JwtFilter;
import com.adtech.iaterap.backend.security.SecurityConfig;
import com.adtech.iaterap.backend.service.AiSuggestionQueueService;
import jakarta.servlet.FilterChain;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doAnswer;

/** HTTP contract tests for the authenticated backend façade around FastAPI. */
@WebMvcTest(AiAdvisoryController.class)
@Import(SecurityConfig.class)
class AiAdvisoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AiAdvisoryClient client;

    @MockBean
    private AiSuggestionQueueService aiSuggestionQueueService;

    @MockBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void forwardsStructuredSuggestionFromIndependentService() throws Exception {
        when(client.suggest(any())).thenReturn(new AiSuggestionResponse(
                "Use a controlled signal-recovery check.", 0.0, "stub", OffsetDateTime.now()));

        mvc.perform(post("/api/ai/suggestions").with(user("tester").roles("TEST_ENGINEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"test_procedure_id\":42,\"test_step_id\":7,\"prompt\":\"Check recovery\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion").value("Use a controlled signal-recovery check."))
                .andExpect(jsonPath("$.confidence").value(0.0))
                .andExpect(jsonPath("$.source").value("stub"))
                .andExpect(jsonPath("$.generated_at").exists());
    }

    @Test
    void returnsGatewayErrorWithoutExposingDownstreamDetails() throws Exception {
        when(client.suggest(any())).thenThrow(new AiAdvisoryUnavailableException("connection refused", null));

        mvc.perform(post("/api/ai/suggestions").with(user("tester").roles("TEST_ENGINEER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI advisory service is temporarily unavailable."));
    }
}
