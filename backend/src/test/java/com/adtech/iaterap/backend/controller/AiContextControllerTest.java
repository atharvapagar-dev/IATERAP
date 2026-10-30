package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.AiCoverageContext;
import com.adtech.iaterap.backend.dto.AiProcedureContextResponse;
import com.adtech.iaterap.backend.security.JwtFilter;
import com.adtech.iaterap.backend.security.SecurityConfig;
import com.adtech.iaterap.backend.service.AiContextService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the AI context boundary is authenticated and read-only. */
@WebMvcTest(AiContextController.class)
@Import(SecurityConfig.class)
class AiContextControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private AiContextService service;
    @MockBean private JwtFilter jwtFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void unauthenticatedContextReadIsRejected() throws Exception {
        mvc.perform(get("/api/ai/context/test-procedure/42"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).getProcedureContext(anyLong());
    }

    @Test
    @WithMockUser
    void authenticatedContextReadReturnsVersionedCoverageShape() throws Exception {
        when(service.getProcedureContext(42L)).thenReturn(new AiProcedureContextResponse(
                "1.0", 42L, "Procedure", "Description", 7L, "DRAFT", 2,
                0, List.of(), new AiCoverageContext(
                Map.of("COVERAGE-INTERFACE", 0D), Map.of("COVERAGE-INTERFACE", 0.15D),
                0D, Map.of(), Map.of(), List.of()), Instant.parse("2026-09-30T00:00:00Z")));

        mvc.perform(get("/api/ai/context/test-procedure/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contract_version").value("1.0"))
                .andExpect(jsonPath("$.data.test_procedure_id").value(42))
                .andExpect(jsonPath("$.data.step_count").value(0))
                .andExpect(jsonPath("$.data.coverage_context.coverage_gaps").isArray())
                .andExpect(jsonPath("$.meta").isMap());
    }

    @Test
    @WithMockUser
    void contextBoundaryHasNoWriteMethod() throws Exception {
        mvc.perform(post("/api/ai/context/test-procedure/42"))
                .andExpect(status().isMethodNotAllowed());
        verify(service, never()).getProcedureContext(anyLong());
    }
}
