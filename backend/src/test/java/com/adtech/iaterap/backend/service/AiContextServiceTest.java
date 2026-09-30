package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.CoverageScoringConfigurationService;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/** Proves the context service reads engineering data without invoking writes. */
class AiContextServiceTest {

    @Test
    void contextReadDoesNotPersistProcedureOrStepData() throws Exception {
        TestProcedureRepository procedures = mock(TestProcedureRepository.class);
        TestStepRepository steps = mock(TestStepRepository.class);
        RuleEngine rules = mock(RuleEngine.class);
        CoverageEngine coverage = mock(CoverageEngine.class);
        CoverageScoringConfigurationService scoring = mock(CoverageScoringConfigurationService.class);
        InterfaceRepository interfaces = mock(InterfaceRepository.class);
        SignalLibraryRepository signals = mock(SignalLibraryRepository.class);
        FaultTemplateRepository faults = mock(FaultTemplateRepository.class);
        FmeaBranchMappingRepository fmea = mock(FmeaBranchMappingRepository.class);

        TestProcedure procedure = new TestProcedure();
        setId(procedure, 42L);
        procedure.setLru(new Lru("LRU-42"));
        procedure.setName("Read-only procedure");
        when(procedures.findById(42L)).thenReturn(Optional.of(procedure));
        when(steps.findByTestProcedureIdOrderByExecutionOrderAsc(42L)).thenReturn(List.of());
        when(rules.evaluate(any(), any(), any())).thenReturn(Map.of());
        when(coverage.calculate(any())).thenReturn(new CoverageScore(Map.of(), 0D, Map.of(), Map.of()));
        when(scoring.activeCoverageRules()).thenReturn(List.of());

        AiContextService service = new AiContextService(
                procedures, steps, rules, coverage, scoring, interfaces, signals, faults, fmea);

        var response = service.getProcedureContext(42L);

        assertEquals(42L, response.testProcedureId());
        assertEquals(0, response.stepCount());
        verify(procedures).findById(42L);
        verify(steps).findByTestProcedureIdOrderByExecutionOrderAsc(42L);
        verify(procedures, never()).save(any());
        verify(steps, never()).save(any());
        verify(procedures, never()).delete(any());
        verify(steps, never()).delete(any());
        verifyNoInteractions(interfaces, signals, faults, fmea);
    }

    @Test
    void missingProcedureIsRejectedWithoutAnyWriteAttempt() {
        TestProcedureRepository procedures = mock(TestProcedureRepository.class);
        when(procedures.findById(999L)).thenReturn(Optional.empty());

        AiContextService service = new AiContextService(
                procedures,
                mock(TestStepRepository.class),
                mock(RuleEngine.class),
                mock(CoverageEngine.class),
                mock(CoverageScoringConfigurationService.class),
                mock(InterfaceRepository.class),
                mock(SignalLibraryRepository.class),
                mock(FaultTemplateRepository.class),
                mock(FmeaBranchMappingRepository.class));

        assertThrows(java.util.NoSuchElementException.class, () -> service.getProcedureContext(999L));
        verify(procedures, never()).save(any());
    }

    private static void setId(TestProcedure procedure, Long id) throws Exception {
        Field field = TestProcedure.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(procedure, id);
    }
}
