package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI-173 / T6-B1: Version Management.
 *
 * <p>Repositories are mocked (no real Postgres in this unit test), so the
 * "database" here is the sequence of save() calls captured via Mockito.
 * This verifies the service's transactional/versioning *logic* --
 * revision numbering, immutability of previously-written snapshots, and
 * no-op-resave suppression -- independent of the persistence layer.</p>
 */
@ExtendWith(MockitoExtension.class)
class TestProcedureServiceTest {

    @Mock private TestProcedureRepository testProcedureRepository;
    @Mock private TestStepRepository testStepRepository;
    @Mock private ProcedureHistoryRepository procedureHistoryRepository;
    @Mock private LruRepository lruRepository;
    @Mock private InterfaceRepository interfaceRepository;
    @Mock private ConnectorRepository connectorRepository;
    @Mock private SignalLibraryRepository signalLibraryRepository;
    @Mock private ProtocolTagRepository protocolTagRepository;
    @Mock private FaultTemplateRepository faultTemplateRepository;

    private TestProcedureService service;

    private Lru lru;
    private Interface iface;
    private Connector connector;
    private SignalLibrary signal;
    private ProtocolTag protocolTag;
    private FaultTemplate faultTemplate;

    @BeforeEach
    void setUp() {
        service = new TestProcedureService(
                testProcedureRepository,
                testStepRepository,
                procedureHistoryRepository,
                lruRepository,
                interfaceRepository,
                connectorRepository,
                signalLibraryRepository,
                protocolTagRepository,
                faultTemplateRepository,
                new ObjectMapper().findAndRegisterModules()
        );

        lru = withId(new Lru("LRU-1"), 1L);
        iface = withId(new Interface("IF-1", lru), 10L);
        connector = withId(new Connector("CONN-1", lru), 20L);
        signal = new SignalLibrary();
        withId(signal, 30L);
        protocolTag = withId(new ProtocolTag("PROTO-1", "0x01", "desc", lru), 40L);
        faultTemplate = new FaultTemplate();
        withId(faultTemplate, 50L);

        lenient().when(lruRepository.findById(1L)).thenReturn(Optional.of(lru));
        lenient().when(interfaceRepository.findById(10L)).thenReturn(Optional.of(iface));
        lenient().when(connectorRepository.findById(20L)).thenReturn(Optional.of(connector));
        lenient().when(signalLibraryRepository.findById(30L)).thenReturn(Optional.of(signal));
        lenient().when(protocolTagRepository.findById(40L)).thenReturn(Optional.of(protocolTag));
        lenient().when(faultTemplateRepository.findById(50L)).thenReturn(Optional.of(faultTemplate));

        // testStepRepository.save assigns an incrementing id, mimicking IDENTITY generation
        lenient().when(testStepRepository.save(any(TestStep.class))).thenAnswer(inv -> {
            TestStep s = inv.getArgument(0);
            if (s.getId() == null) {
                withId(s, System.nanoTime());
            }
            return s;
        });
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private TestProcedureRequest requestWithOneStep(String procedureName, String behaviour) {
        TestStepRequest step = new TestStepRequest();
        step.setInterfaceId(10L);
        step.setConnectorId(20L);
        step.setSignalLibraryId(30L);
        step.setProtocolTagId(40L);
        step.setFaultCategoryId(50L);
        step.setExpectedBehaviour(behaviour);
        step.setAcceptanceCriteria("criteria");
        step.setPassFailCriteria("pass/fail");
        step.setExecutionOrder(1);

        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(1L);
        request.setName(procedureName);
        request.setDescription("desc");
        request.setSteps(List.of(step));
        return request;
    }

    @Test
    void firstSaveCreatesRevisionOne() {
        when(testProcedureRepository.save(any(TestProcedure.class))).thenAnswer(inv -> {
            TestProcedure p = inv.getArgument(0);
            if (p.getId() == null) withId(p, 100L);
            return p;
        });

        List<TestStep> stepsHolder = new ArrayList<>();
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(100L))
                .thenAnswer(inv -> new ArrayList<>(stepsHolder));
        when(testStepRepository.save(any(TestStep.class))).thenAnswer(inv -> {
            TestStep s = inv.getArgument(0);
            if (s.getId() == null) withId(s, System.nanoTime());
            stepsHolder.removeIf(existing -> existing.getId().equals(s.getId()));
            stepsHolder.add(s);
            return s;
        });

        TestProcedureResponse response = service.save(requestWithOneStep("Proc A", "behaviour v1"));

        assertThat(response.getVersionNumber()).isEqualTo(1);

        ArgumentCaptor<ProcedureHistory> captor = ArgumentCaptor.forClass(ProcedureHistory.class);
        verify(procedureHistoryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getVersionNumber()).isEqualTo(1);
        assertThat(captor.getValue().getTestProcedureId()).isEqualTo(100L);
        assertThat(captor.getValue().getStepsSnapshot()).contains("behaviour v1");
    }

    @Test
    void secondContentChangingSaveCreatesDistinctSecondRevision() {
        TestProcedure procedure = withId(new TestProcedure(), 200L);
        procedure.setLru(lru);
        procedure.setName("Proc B");
        procedure.setDescription("desc");
        procedure.setVersionNumber(1);

        when(testProcedureRepository.findById(200L)).thenReturn(Optional.of(procedure));
        when(testProcedureRepository.save(any(TestProcedure.class))).thenAnswer(inv -> inv.getArgument(0));

        // Revision 1 already exists with the "before" content.
        List<TestStep> stepsHolder = new ArrayList<>();
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(200L))
                .thenAnswer(inv -> new ArrayList<>(stepsHolder));
        when(testStepRepository.save(any(TestStep.class))).thenAnswer(inv -> {
            TestStep s = inv.getArgument(0);
            if (s.getId() == null) withId(s, System.nanoTime());
            stepsHolder.removeIf(existing -> existing.getId().equals(s.getId()));
            stepsHolder.add(s);
            return s;
        });

        ProcedureHistory revisionOne = new ProcedureHistory();
        revisionOne.setTestProcedureId(200L);
        revisionOne.setVersionNumber(1);
        revisionOne.setName("Proc B");
        revisionOne.setDescription("desc");
        revisionOne.setLruId(1L);
        revisionOne.setApprovalStatus("DRAFT");
        revisionOne.setStepsSnapshot("[]"); // no steps yet in this fixture's revision 1
        when(procedureHistoryRepository.findTopByTestProcedureIdOrderByVersionNumberDesc(200L))
                .thenReturn(Optional.of(revisionOne));

        TestProcedureResponse response = service.update(200L, requestWithOneStep("Proc B", "behaviour v2"));

        assertThat(response.getVersionNumber()).isEqualTo(2);

        ArgumentCaptor<ProcedureHistory> captor = ArgumentCaptor.forClass(ProcedureHistory.class);
        verify(procedureHistoryRepository, times(1)).save(captor.capture());
        ProcedureHistory revisionTwo = captor.getValue();

        assertThat(revisionTwo.getVersionNumber()).isEqualTo(2);
        assertThat(revisionTwo.getStepsSnapshot()).contains("behaviour v2");

        // Revision 1's own snapshot object was never touched by the second save.
        assertThat(revisionOne.getVersionNumber()).isEqualTo(1);
        assertThat(revisionOne.getStepsSnapshot()).isEqualTo("[]");
        assertThat(revisionOne).isNotSameAs(revisionTwo);
    }

    @Test
    void noOpResaveDoesNotCreateANewRevision() {
        TestProcedure procedure = withId(new TestProcedure(), 300L);
        procedure.setLru(lru);
        procedure.setName("Proc C");
        procedure.setDescription("desc");
        procedure.setVersionNumber(1);

        when(testProcedureRepository.findById(300L)).thenReturn(Optional.of(procedure));
        when(testProcedureRepository.save(any(TestProcedure.class))).thenAnswer(inv -> inv.getArgument(0));

        // A single pre-existing TestStep, already matching what the identical
        // update request below will produce, so replaceSteps() is a pure
        // in-place update (same id) and the recomputed snapshot is identical
        // to the "latest" revision's snapshot.
        TestStep existingStep = new TestStep();
        withId(existingStep, 999L);
        existingStep.setTestProcedure(procedure);
        existingStep.setInterfaceEntity(iface);
        existingStep.setConnector(connector);
        existingStep.setSignalLibrary(signal);
        existingStep.setProtocolTag(protocolTag);
        existingStep.setFaultCategory(faultTemplate);
        existingStep.setExpectedBehaviour("same behaviour");
        existingStep.setAcceptanceCriteria("criteria");
        existingStep.setPassFailCriteria("pass/fail");
        existingStep.setExecutionOrder(1);

        List<TestStep> stepsHolder = new ArrayList<>(List.of(existingStep));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(300L))
                .thenAnswer(inv -> new ArrayList<>(stepsHolder));
        when(testStepRepository.findById(999L)).thenReturn(Optional.of(existingStep));
        when(testStepRepository.save(any(TestStep.class))).thenAnswer(inv -> inv.getArgument(0));

        TestStepRequest sameStepRequest = new TestStepRequest();
        sameStepRequest.setId(999L);
        sameStepRequest.setInterfaceId(10L);
        sameStepRequest.setConnectorId(20L);
        sameStepRequest.setSignalLibraryId(30L);
        sameStepRequest.setProtocolTagId(40L);
        sameStepRequest.setFaultCategoryId(50L);
        sameStepRequest.setExpectedBehaviour("same behaviour");
        sameStepRequest.setAcceptanceCriteria("criteria");
        sameStepRequest.setPassFailCriteria("pass/fail");
        sameStepRequest.setExecutionOrder(1);

        TestProcedureRequest sameRequest = new TestProcedureRequest();
        sameRequest.setLruId(1L);
        sameRequest.setName("Proc C");
        sameRequest.setDescription("desc");
        sameRequest.setSteps(List.of(sameStepRequest));

        String matchingSnapshot = new ObjectMapper().valueToTree(
                List.of(com.adtech.iaterap.backend.dto.TestStepResponse.fromEntity(existingStep))
        ).toString();

        ProcedureHistory latest = new ProcedureHistory();
        latest.setTestProcedureId(300L);
        latest.setVersionNumber(1);
        latest.setName("Proc C");
        latest.setDescription("desc");
        latest.setLruId(1L);
        latest.setLruName("LRU-1");
        latest.setApprovalStatus("DRAFT");
        latest.setStepsSnapshot(matchingSnapshot);
        when(procedureHistoryRepository.findTopByTestProcedureIdOrderByVersionNumberDesc(300L))
                .thenReturn(Optional.of(latest));

        TestProcedureResponse response = service.update(300L, sameRequest);

        assertThat(response.getVersionNumber()).isEqualTo(1);
        verify(procedureHistoryRepository, never()).save(any());
    }

    @Test
    void historyRetrievalReturnsRepositoryOrderedByVersion() {
        TestProcedure procedure = withId(new TestProcedure(), 400L);
        when(testProcedureRepository.findById(400L)).thenReturn(Optional.of(procedure));

        ProcedureHistory v1 = new ProcedureHistory();
        v1.setTestProcedureId(400L);
        v1.setVersionNumber(1);
        ProcedureHistory v2 = new ProcedureHistory();
        v2.setTestProcedureId(400L);
        v2.setVersionNumber(2);

        when(procedureHistoryRepository.findByTestProcedureIdOrderByVersionNumberAsc(400L))
                .thenReturn(List.of(v1, v2));

        List<com.adtech.iaterap.backend.dto.ProcedureHistoryResponse> history = service.getHistory(400L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getVersionNumber()).isEqualTo(1);
        assertThat(history.get(1).getVersionNumber()).isEqualTo(2);
    }

}
