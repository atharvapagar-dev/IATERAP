package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.security.JwtFilter;
import com.adtech.iaterap.backend.security.SecurityConfig;
import com.adtech.iaterap.backend.service.SyntheticFaultInjectionService;
import com.adtech.iaterap.backend.service.ValidationIngestionService;
import com.adtech.iaterap.backend.validation.SyntheticInjectionScenario;
import com.adtech.iaterap.backend.validation.ValidationRunStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies role-based access control and report endpoints on ValidationController (T13-B1, T13-B2).
 */
@WebMvcTest(ValidationController.class)
@Import(SecurityConfig.class)
class ValidationControllerAuthorizationTest {

    @Autowired private MockMvc mvc;
    @MockBean private ValidationIngestionService ingestionService;
    @MockBean private ValidationRunRepository runRepository;
    @MockBean private ValidationReadingRepository readingRepository;
    @MockBean private ProcedureHistoryRepository historyRepository;
    @MockBean private SyntheticFaultInjectionService syntheticFaultInjectionService;
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
    @DisplayName("Unauthenticated request to trigger synthetic fault injection is rejected with 401")
    void unauthenticatedSyntheticInjectionIsRejected() throws Exception {
        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1")
                        .param("scenario", "NOMINAL"))
                .andExpect(status().isUnauthorized());

        verify(syntheticFaultInjectionService, never()).run(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER role cannot trigger synthetic fault injection (403 Forbidden)")
    void viewerCannotTriggerSyntheticInjection() throws Exception {
        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1")
                        .param("scenario", "FAULT_INJECTED"))
                .andExpect(status().isForbidden());

        verify(syntheticFaultInjectionService, never()).run(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "FIELD_ENGINEER")
    @DisplayName("FIELD_ENGINEER role cannot trigger synthetic fault injection (403 Forbidden)")
    void fieldEngineerCannotTriggerSyntheticInjection() throws Exception {
        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1")
                        .param("scenario", "FAULT_INJECTED"))
                .andExpect(status().isForbidden());

        verify(syntheticFaultInjectionService, never()).run(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "TEST_ENGINEER")
    @DisplayName("TEST_ENGINEER role can trigger synthetic fault injection (200 OK)")
    void testEngineerCanTriggerSyntheticInjection() throws Exception {
        ValidationRun dummyRun = new ValidationRun();
        dummyRun.setProcedureHistoryId(1L);
        dummyRun.setStatus(ValidationRunStatus.VALIDATED);
        when(syntheticFaultInjectionService.run(eq(1L), any(), any(), any(), any(), any())).thenReturn(dummyRun);

        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1")
                        .param("scenario", "FAULT_INJECTED")
                        .param("faultCategory", "OUT_OF_RANGE"))
                .andExpect(status().isOk());

        verify(syntheticFaultInjectionService).run(eq(1L), eq(SyntheticInjectionScenario.FAULT_INJECTED), eq("OUT_OF_RANGE"), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "RELIABILITY_ENGINEER")
    @DisplayName("RELIABILITY_ENGINEER role can trigger synthetic fault injection (200 OK)")
    void reliabilityEngineerCanTriggerSyntheticInjection() throws Exception {
        ValidationRun dummyRun = new ValidationRun();
        dummyRun.setProcedureHistoryId(1L);
        dummyRun.setStatus(ValidationRunStatus.VALIDATED);
        when(syntheticFaultInjectionService.run(eq(1L), any(), any(), any(), any(), any())).thenReturn(dummyRun);

        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1")
                        .param("scenario", "NOMINAL"))
                .andExpect(status().isOk());

        verify(syntheticFaultInjectionService).run(eq(1L), eq(SyntheticInjectionScenario.NOMINAL), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ENGINEER")
    @DisplayName("SYSTEM_ENGINEER role can trigger synthetic fault injection (200 OK)")
    void systemEngineerCanTriggerSyntheticInjection() throws Exception {
        ValidationRun dummyRun = new ValidationRun();
        dummyRun.setProcedureHistoryId(1L);
        when(syntheticFaultInjectionService.run(any(), any(), any(), any(), any(), any())).thenReturn(dummyRun);

        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1"))
                .andExpect(status().isOk());

        verify(syntheticFaultInjectionService).run(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN role can trigger synthetic fault injection (200 OK)")
    void adminCanTriggerSyntheticInjection() throws Exception {
        ValidationRun dummyRun = new ValidationRun();
        dummyRun.setProcedureHistoryId(1L);
        when(syntheticFaultInjectionService.run(any(), any(), any(), any(), any(), any())).thenReturn(dummyRun);

        mvc.perform(post("/api/validation/runs/synthetic")
                        .with(csrf())
                        .param("procedureHistoryId", "1")
                        .param("targetLruId", "5")
                        .param("targetStepId", "101"))
                .andExpect(status().isOk());

        verify(syntheticFaultInjectionService).run(eq(1L), any(), any(), eq(5L), eq(101L), any());
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can retrieve consolidated validation report")
    void authenticatedUserCanGetConsolidatedReport() throws Exception {
        ValidationRun run = new ValidationRun();
        run.setProcedureHistoryId(10L);
        run.setSourceFilename("synthetic-fault_injected-out_of_range-abc.json");
        run.setSourceFormat("JSON");
        run.setRecordedBy("validator");
        run.setStatus(ValidationRunStatus.VALIDATED);
        run.setTotalReadings(2);
        run.setMatchedCount(1);
        run.setMismatchedCount(1);

        ProcedureHistory history = new ProcedureHistory();
        history.setName("Engine Throttle Test");
        history.setVersionNumber(2);
        history.setLruId(7L);
        history.setLruName("EEC-1");

        when(runRepository.findById(1L)).thenReturn(Optional.of(run));
        when(historyRepository.findById(10L)).thenReturn(Optional.of(history));
        when(readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(1L)).thenReturn(List.of());

        mvc.perform(get("/api/validation/runs/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getId()))
                .andExpect(jsonPath("$.procedureName").value("Engine Throttle Test"))
                .andExpect(jsonPath("$.procedureVersionNumber").value(2))
                .andExpect(jsonPath("$.lruName").value("EEC-1"))
                .andExpect(jsonPath("$.synthetic").value(true))
                .andExpect(jsonPath("$.totalReadings").value(2))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.mismatchedCount").value(1))
                .andExpect(jsonPath("$.passRatePercentage").value(50.0));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can retrieve report via /api/validation/reports/{runId} alias")
    void authenticatedUserCanGetReportViaReportsAlias() throws Exception {
        ValidationRun run = new ValidationRun();
        run.setProcedureHistoryId(10L);
        run.setSourceFilename("synthetic-nominal-abc.json");
        run.setSourceFormat("JSON");
        run.setStatus(ValidationRunStatus.VALIDATED);

        when(runRepository.findById(1L)).thenReturn(Optional.of(run));
        when(readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(1L)).thenReturn(List.of());

        mvc.perform(get("/api/validation/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synthetic").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list reports by procedureHistoryId")
    void authenticatedUserCanListReportsByProcedureHistoryId() throws Exception {
        ValidationRun run = new ValidationRun();
        run.setProcedureHistoryId(10L);
        run.setSourceFilename("evidence.csv");
        run.setSourceFormat("CSV");
        run.setStatus(ValidationRunStatus.VALIDATED);

        when(runRepository.findByProcedureHistoryIdOrderByUploadedAtDesc(10L)).thenReturn(List.of(run));
        when(historyRepository.findById(10L)).thenReturn(Optional.empty());
        when(readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(any())).thenReturn(List.of());

        mvc.perform(get("/api/validation/reports").param("procedureHistoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].procedureHistoryId").value(10L))
                .andExpect(jsonPath("$[0].synthetic").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Requesting report for non-existent run returns 400 Bad Request")
    void nonExistentRunReturnsBadRequest() throws Exception {
        when(runRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/validation/runs/999/report"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation run not found: 999"));
    }
}
