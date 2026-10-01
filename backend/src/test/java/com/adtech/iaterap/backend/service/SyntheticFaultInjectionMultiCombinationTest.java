package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.validation.SyntheticInjectionScenario;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI-240 / T13-QA: exercises independent LRU and fault-category combinations.
 *
 * <p>Each case represents an approved immutable revision. The test proves the
 * generated evidence contains only that revision's step identity and that the
 * requested synthetic scenario is passed into the normal ingestion boundary.</p>
 */
@ExtendWith(MockitoExtension.class)
class SyntheticFaultInjectionMultiCombinationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private ProcedureHistoryRepository histories;

    @Mock
    private ValidationIngestionService ingestion;

    private SyntheticFaultInjectionService service;

    @BeforeEach
    void setUp() {
        service = new SyntheticFaultInjectionService(histories, ingestion, JSON);
    }

    @ParameterizedTest(name = "{0} / {1} / {2}")
    @MethodSource("approvedCombinations")
    void generatesIsolatedEvidenceForEachLruFaultCategoryCombination(
            String lruName,
            String faultCategory,
            SyntheticInjectionScenario scenario,
            long historyId,
            long stepId,
            double expectedValue) throws Exception {
        ProcedureHistory history = approvedHistory(historyId, stepSnapshot(stepId, lruName, faultCategory));
        ValidationRun persistedRun = new ValidationRun();
        when(histories.findById(historyId)).thenReturn(Optional.of(history));
        when(ingestion.ingest(eq(historyId), any(String.class), eq("JSON"), eq("qa-engineer"), any(byte[].class)))
                .thenReturn(persistedRun);

        assertSame(persistedRun, service.run(historyId, scenario, "qa-engineer"));

        ArgumentCaptor<String> filename = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> fixture = ArgumentCaptor.forClass(byte[].class);
        verify(ingestion).ingest(eq(historyId), filename.capture(), eq("JSON"), eq("qa-engineer"), fixture.capture());

        assertTrue(filename.getValue().startsWith("synthetic-" + scenario.name().toLowerCase() + "-"));
        JsonNode reading = JSON.readTree(fixture.getValue()).get(0);
        assertEquals(stepId, reading.path("testStepId").asLong(),
                "The fixture must remain bound to the selected procedure revision only");
        assertEquals("VOLT", reading.path("observedUnit").asText());
        assertEquals(expectedValue, reading.path("observedValue").asDouble(), 0.000001);
        assertTrue(reading.hasNonNull("_injectionRunId"));
    }

    @Test
    void rejectsAnyCombinationWhoseRevisionIsNotApproved() {
        long historyId = 909L;
        ProcedureHistory draft = new ProcedureHistory();
        draft.setApprovalStatus("DRAFT");
        draft.setStepsSnapshot(stepSnapshot(9001L, "LRU-DRAFT", "FAULT-DRAFT"));
        when(histories.findById(historyId)).thenReturn(Optional.of(draft));

        assertThrows(ValidationFixtureException.class,
                () -> service.run(historyId, SyntheticInjectionScenario.FAULT_INJECTED, "qa-engineer"));
        verifyNoInteractions(ingestion);
    }

    private static Stream<Arguments> approvedCombinations() {
        return Stream.of(
                Arguments.of("LRU-FCC", "OPEN_CIRCUIT", SyntheticInjectionScenario.NOMINAL,
                        101L, 1001L, 15.0),
                Arguments.of("LRU-FCC", "OPEN_CIRCUIT", SyntheticInjectionScenario.FAULT_INJECTED,
                        102L, 1002L, 30.0),
                Arguments.of("LRU-PDU", "OVERCURRENT", SyntheticInjectionScenario.NOMINAL,
                        201L, 2001L, 15.0),
                Arguments.of("LRU-PDU", "OVERCURRENT", SyntheticInjectionScenario.FAULT_INJECTED,
                        202L, 2002L, 30.0),
                Arguments.of("LRU-RADIO", "SIGNAL_LOSS", SyntheticInjectionScenario.NOMINAL,
                        301L, 3001L, 15.0),
                Arguments.of("LRU-RADIO", "SIGNAL_LOSS", SyntheticInjectionScenario.FAULT_INJECTED,
                        302L, 3002L, 30.0)
        );
    }

    private static ProcedureHistory approvedHistory(long historyId, String snapshot) {
        ProcedureHistory history = new ProcedureHistory();
        history.setTestProcedureId(historyId);
        history.setVersionNumber(2);
        history.setName("Approved synthetic procedure " + historyId);
        history.setLruId(historyId);
        history.setApprovalStatus("APPROVED");
        history.setStepsSnapshot(snapshot);
        history.setCreatedBy("qa-engineer");
        return history;
    }

    private static String stepSnapshot(long stepId, String lruName, String faultCategory) {
        return """
                [{
                  "id": %d,
                  "lruName": "%s",
                  "faultCategory": {"code": "%s"},
                  "expectedRange": "10..20",
                  "expectedUnit": "VOLT",
                  "expectedBehaviour": "Nominal voltage within tolerance"
                }]
                """.formatted(stepId, lruName, faultCategory);
    }
}
