package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.approval.ApprovalStateMachine;
import com.adtech.iaterap.backend.approval.CoverageThresholdNotMetException;
import com.adtech.iaterap.backend.approval.InvalidApprovalTransitionException;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.entity.CoverageReport;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcedureApprovalCoverageGateTest {

    @Mock private TestProcedureRepository procedures;
    @Mock private TestStepRepository steps;
    @Mock private ProcedureHistoryRepository histories;
    @Mock private LruRepository lrus;
    @Mock private InterfaceRepository interfaces;
    @Mock private ConnectorRepository connectors;
    @Mock private SignalLibraryRepository signals;
    @Mock private ProtocolTagRepository protocols;
    @Mock private FaultTemplateRepository faults;
    @Mock private RuleEngine ruleEngine;
    @Mock private ProcedureSnapshotService snapshots;

    private TestProcedureService service;

    @BeforeEach
    void setUp() {
        service = new TestProcedureService(procedures, steps, histories, lrus, interfaces,
                connectors, signals, protocols, faults,
                new ObjectMapper().findAndRegisterModules(), null, ruleEngine);
        service.setProcedureSnapshotService(snapshots);
    }

    @Test
    void approveTransitionsInReviewProcedureWhenFrozenReportMeetsThresholds() {
        TestProcedure procedure = inReviewProcedure();
        when(procedures.findById(1L)).thenReturn(Optional.of(procedure));
        when(steps.findByTestProcedureIdOrderByExecutionOrderAsc(1L)).thenReturn(java.util.List.of());
        when(snapshots.getReport(1L, 2)).thenReturn(report("{\"thresholdResults\":{\"COVERAGE-INTERFACE\":true,\"COVERAGE-SIGNAL\":true,\"COVERAGE-REQUIREMENT\":null}}"));
        when(procedures.save(procedure)).thenReturn(procedure);

        var response = service.approve(1L);

        assertThat(response.getApprovalStatus()).isEqualTo("APPROVED");
        verify(snapshots).getReport(1L, 2);
        verify(procedures).save(procedure);
    }

    @Test
    void approveRejectsBelowThresholdFrozenReportWithoutChangingStateOrWritingHistory() {
        TestProcedure procedure = inReviewProcedure();
        when(procedures.findById(1L)).thenReturn(Optional.of(procedure));
        when(snapshots.getReport(1L, 2)).thenReturn(report("{\"thresholdResults\":{\"COVERAGE-FAULT\":false}}"));

        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(CoverageThresholdNotMetException.class)
                .hasMessageContaining("COVERAGE-FAULT");

        assertThat(procedure.getApprovalStatus()).isEqualTo("IN_REVIEW");
        verify(procedures, never()).save(procedure);
        verify(histories, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void approveUsesTheReviewedVersionFrozenReportRatherThanMutableCoverageInputs() {
        TestProcedure procedure = inReviewProcedure();
        when(procedures.findById(1L)).thenReturn(Optional.of(procedure));
        // This report remains the decision input even if shared assets later change.
        when(snapshots.getReport(1L, 2)).thenReturn(report("{\"thresholdResults\":{\"COVERAGE-INTERFACE\":true}}"));
        when(steps.findByTestProcedureIdOrderByExecutionOrderAsc(1L)).thenReturn(java.util.List.of());
        when(procedures.save(procedure)).thenReturn(procedure);

        service.approve(1L);

        verify(snapshots).getReport(1L, 2);
        verify(procedures).save(procedure);
    }

    @Test
    void existingStateMachineRulesStillRejectApprovalOutsideReview() {
        TestProcedure procedure = inReviewProcedure();
        procedure.setApprovalStatus("DRAFT");
        when(procedures.findById(1L)).thenReturn(Optional.of(procedure));

        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(InvalidApprovalTransitionException.class)
                .hasMessageContaining("DRAFT");

        verify(snapshots, never()).getReport(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(procedures, never()).save(procedure);
    }

    private static CoverageReport report(String payload) {
        return new CoverageReport(10L, 1L, 2, payload);
    }

    private static TestProcedure inReviewProcedure() {
        TestProcedure procedure = new TestProcedure();
        setId(procedure, 1L);
        procedure.setApprovalStatus("IN_REVIEW");
        procedure.setVersionNumber(2);
        Lru lru = new Lru();
        setId(lru, 100L);
        lru.setName("LRU");
        procedure.setLru(lru);
        return procedure;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
