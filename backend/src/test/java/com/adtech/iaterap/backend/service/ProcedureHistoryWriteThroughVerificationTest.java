package com.adtech.iaterap.backend.service;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("synthetic-data")
class ProcedureHistoryWriteThroughVerificationTest {

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
        request.setDescription("AI-176 verification fixture");
        request.setSteps(List.of(buildStepRequest(behaviour, 1)));
        return request;
    }

    @Test
    void savingNewProcedureWritesRevisionOneToProcedureHistoryTable() {
        TestProcedureResponse response = testProcedureService.save(
                buildProcedureRequest("AI-176 Save Verification", "behaviour v1"));
        createdProcedureIds.add(response.getId());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version_number, steps_snapshot, created_by FROM procedure_history " +
                "WHERE test_procedure_id = ?", response.getId());
        if (!rows.isEmpty()) {
            Object snap = rows.get(0).get("steps_snapshot");

        }

        assertEquals(1, rows.size(), "Exactly one procedure_history row should exist after the first save");
        assertEquals(1, ((Number) rows.get(0).get("version_number")).intValue());
        assertTrue(rows.get(0).get("steps_snapshot").toString().contains("behaviour v1"),
                "steps_snapshot should contain the actual saved step content");
        assertNotNull(rows.get(0).get("created_by"));
    }

    @Test
    void contentChangingUpdateWritesASecondRevisionRow() {
        TestProcedureResponse created = testProcedureService.save(
                buildProcedureRequest("AI-176 Update Verification", "behaviour v1"));
        createdProcedureIds.add(created.getId());

        testProcedureService.update(created.getId(),
                buildProcedureRequest("AI-176 Update Verification", "behaviour v2"));

        Integer historyRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM procedure_history WHERE test_procedure_id = ?",
                Integer.class, created.getId());

        assertEquals(2, historyRowCount);

        List<Integer> versionNumbers = jdbcTemplate.queryForList(
                "SELECT version_number FROM procedure_history WHERE test_procedure_id = ? ORDER BY version_number",
                Integer.class, created.getId());
        assertEquals(List.of(1, 2), versionNumbers);
    }

    @Test
    void noOpResaveDoesNotAddAnExtraProcedureHistoryRow() {
        TestProcedureResponse created = testProcedureService.save(
                buildProcedureRequest("AI-176 NoOp Verification", "behaviour v1"));
        createdProcedureIds.add(created.getId());

        TestStepRequest sameStep = buildStepRequest("behaviour v1", 1);
        Long existingStepId = testStepRepository
                .findByTestProcedureIdOrderByExecutionOrderAsc(created.getId())
                .get(0).getId();
        sameStep.setId(existingStepId);

        TestProcedureRequest sameRequest = new TestProcedureRequest();
        sameRequest.setLruId(created.getLruId());
        sameRequest.setName(created.getName());
        sameRequest.setDescription(created.getDescription());
        sameRequest.setSteps(List.of(sameStep));

        testProcedureService.update(created.getId(), sameRequest);

        Integer historyRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM procedure_history WHERE test_procedure_id = ?",
                Integer.class, created.getId());

        assertEquals(1, historyRowCount);
    }
}