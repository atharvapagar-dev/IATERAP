package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("synthetic-data")
class TwoRevisionRetrievabilityTest {

    @Autowired private TestProcedureService testProcedureService;
    @Autowired private TestProcedureRepository testProcedureRepository;
    @Autowired private TestStepRepository testStepRepository;
    @Autowired private LruRepository lruRepository;
    @Autowired private InterfaceRepository interfaceRepository;
    @Autowired private ConnectorRepository connectorRepository;
    @Autowired private SignalLibraryRepository signalLibraryRepository;
    @Autowired private ProtocolTagRepository protocolTagRepository;
    @Autowired private FaultTemplateRepository faultTemplateRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<Long> createdProcedureIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long procedureId : createdProcedureIds) {
            jdbcTemplate.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", procedureId);
            testStepRepository.deleteAll(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));
            testProcedureRepository.deleteById(procedureId);
        }
        createdProcedureIds.clear();
    }

    private TestStepRequest buildStepRequest(String behaviour, int order) {
        Lru lru = lruRepository.findAll().get(0);
        Interface iface = interfaceRepository.findAll().stream()
                .filter(i -> i.getLru().getId().equals(lru.getId()))
                .findFirst()
                .orElseThrow();
        Connector connector = connectorRepository.findByLruId(lru.getId()).get(0);
        SignalLibrary signal = signalLibraryRepository.findByLruId(lru.getId()).get(0);
        ProtocolTag protocolTag = protocolTagRepository.findByLruId(lru.getId()).get(0);
        FaultTemplate fault = faultTemplateRepository.findByLruId(lru.getId()).get(0);

        TestStepRequest step = new TestStepRequest();
        step.setInterfaceId(iface.getId());
        step.setConnectorId(connector.getId());
        step.setSignalLibraryId(signal.getId());
        step.setProtocolTagId(protocolTag.getId());
        step.setFaultCategoryId(fault.getId());
        step.setExpectedBehaviour(behaviour);
        step.setAcceptanceCriteria("criteria");
        step.setPassFailCriteria("pass/fail");
        step.setExecutionOrder(order);
        return step;
    }

    private TestProcedureRequest buildProcedureRequest(String name, String behaviour) {
        Lru lru = lruRepository.findAll().get(0);
        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName(name);
        request.setDescription("AI-177 retrievability fixture");
        request.setSteps(List.of(buildStepRequest(behaviour, 1)));
        return request;
    }

    @Test
    void twoConsecutiveSavesProduceTwoDistinctRetrievableRevisions() {
        TestProcedureResponse created = testProcedureService.save(
                buildProcedureRequest("AI-177 Retrievability Procedure", "behaviour v1"));
        createdProcedureIds.add(created.getId());

        TestProcedureResponse updated = testProcedureService.update(created.getId(),
                buildProcedureRequest("AI-177 Retrievability Procedure", "behaviour v2"));

        List<ProcedureHistoryResponse> history = testProcedureService.getHistory(updated.getId());
        assertEquals(2, history.size(), "Two consecutive content-changing saves should produce two revisions");
        assertEquals(1, history.get(0).getVersionNumber());
        assertEquals(2, history.get(1).getVersionNumber());
        assertNotEquals(history.get(0).getStepsSnapshot(), history.get(1).getStepsSnapshot(),
                "The two revisions must carry genuinely distinct step content, not two copies of the same snapshot");

        ProcedureHistoryResponse revisionOne = testProcedureService.getRevision(updated.getId(), 1);
        ProcedureHistoryResponse revisionTwo = testProcedureService.getRevision(updated.getId(), 2);

        assertEquals(1, revisionOne.getVersionNumber());
        assertTrue(revisionOne.getStepsSnapshot().contains("behaviour v1"),
                "Revision 1 should retrieve the original step content");

        assertEquals(2, revisionTwo.getVersionNumber());
        assertTrue(revisionTwo.getStepsSnapshot().contains("behaviour v2"),
                "Revision 2 should retrieve the updated step content");

        assertEquals(2, updated.getVersionNumber());
    }

    @Test
    void retrievingANonExistentRevisionNumberThrows() {
        TestProcedureResponse created = testProcedureService.save(
                buildProcedureRequest("AI-177 Missing Revision Procedure", "behaviour v1"));
        createdProcedureIds.add(created.getId());

        assertThrows(NoSuchElementException.class,
                () -> testProcedureService.getRevision(created.getId(), 99),
                "Requesting a revision number that was never written should fail loudly, not return null");
    }
}
