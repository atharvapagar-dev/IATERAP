package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI-185 [T7-SH]: procedure_history field-level data verification.
 *
 * <p>No new tables. This confirms that a procedure_history row -- both its
 * own columns and the JSON steps_snapshot -- carries enough field-level
 * data for a future version-comparison diff engine to render a full,
 * point-in-time-accurate revision without re-querying any live table.</p>
 *
 * <p>While verifying this, found and closed a real gap: every reference
 * field inside steps_snapshot already stores both an id AND the resolved
 * display name at snapshot time (see TestStepResponse: interfaceName,
 * connectorName, signalLibraryName, protocolTagName, faultCategoryName) --
 * except procedure_history's own lru_id, which had no matching lru_name.
 * A diff engine reading procedure_history alone could not have shown which
 * LRU a historical revision belonged to once the live LRU was renamed.
 * Closed via V18 (lru_name column) + ProcedureHistory/TestProcedureService/
 * ProcedureHistoryResponse updates, verified below alongside the fields
 * that already worked correctly.</p>
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class ProcedureHistoryFieldVerificationTest {

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
    @Autowired private ObjectMapper objectMapper;

    private final List<Long> createdProcedureIds = new ArrayList<>();
    private String originalLruName;
    private Long renamedLruId;
    private String originalInterfaceName;
    private Long renamedInterfaceId;

    @AfterEach
    void cleanUp() {
        for (Long procedureId : createdProcedureIds) {
            jdbcTemplate.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", procedureId);
            testStepRepository.deleteAll(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));
            testProcedureRepository.deleteById(procedureId);
        }
        createdProcedureIds.clear();

        // Restore anything renamed mid-test so later tests/seeded data are unaffected.
        if (renamedLruId != null && originalLruName != null) {
            Lru lru = lruRepository.findById(renamedLruId).orElseThrow();
            lru.setName(originalLruName);
            lruRepository.save(lru);
        }
        if (renamedInterfaceId != null && originalInterfaceName != null) {
            Interface iface = interfaceRepository.findById(renamedInterfaceId).orElseThrow();
            iface.setName(originalInterfaceName);
            interfaceRepository.save(iface);
        }
    }

    // ---- fixtures ----

    private Lru firstLru() {
        return lruRepository.findAll().get(0);
    }

    private Interface firstInterfaceForLru(Lru lru) {
        return interfaceRepository.findAll().stream()
                .filter(i -> i.getLru().getId().equals(lru.getId()))
                .findFirst()
                .orElseThrow();
    }

    private TestStepRequest buildStepRequest(Lru lru, Interface iface, String behaviour, int order) {
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

    // ---- tests ----

    @Test
    void procedureHistoryRow_containsEveryFieldADiffEngineNeeds() throws Exception {
        Lru lru = firstLru();
        Interface iface = firstInterfaceForLru(lru);

        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName("AI-185 Field Verification Procedure");
        request.setDescription("checks every field a diff engine would need");
        request.setSteps(List.of(buildStepRequest(lru, iface, "step behaviour", 1)));

        TestProcedureResponse created = testProcedureService.save(request);
        createdProcedureIds.add(created.getId());

        ProcedureHistoryResponse revision = testProcedureService.getRevision(created.getId(), 1);

        // Top-level procedure_history columns.
        assertNotNull(revision.getId());
        assertEquals(created.getId(), revision.getTestProcedureId());
        assertEquals(1, revision.getVersionNumber());
        assertEquals("AI-185 Field Verification Procedure", revision.getName());
        assertEquals("checks every field a diff engine would need", revision.getDescription());
        assertEquals(lru.getId(), revision.getLruId());
        assertEquals(lru.getName(), revision.getLruName(), "lru_name must be captured alongside lru_id");
        assertNotNull(revision.getApprovalStatus());
        assertNotNull(revision.getCreatedBy());
        assertNotNull(revision.getCreatedAt());
        assertNotNull(revision.getStepsSnapshot());

        // steps_snapshot must carry both id and resolved display name for
        // every typed reference, plus every content field -- not just FK ids.
        JsonNode steps = objectMapper.readTree(revision.getStepsSnapshot());
        assertTrue(steps.isArray());
        assertEquals(1, steps.size());
        JsonNode step = steps.get(0);

        assertTrue(step.has("id"));
        assertTrue(step.hasNonNull("interfaceId"));
        assertTrue(step.hasNonNull("interfaceName"));
        assertTrue(step.hasNonNull("connectorId"));
        assertTrue(step.hasNonNull("connectorName"));
        assertTrue(step.hasNonNull("signalLibraryId"));
        assertTrue(step.hasNonNull("signalLibraryName"));
        assertTrue(step.hasNonNull("protocolTagId"));
        assertTrue(step.hasNonNull("protocolTagName"));
        assertTrue(step.hasNonNull("faultCategoryId"));
        assertTrue(step.hasNonNull("faultCategoryName"));
        assertEquals("step behaviour", step.get("expectedBehaviour").asText());
        assertTrue(step.hasNonNull("acceptanceCriteria"));
        assertTrue(step.hasNonNull("passFailCriteria"));
        assertTrue(step.hasNonNull("executionOrder"));
        assertTrue(step.has("approvalStatus"));
        assertTrue(step.has("versionNumber"));
    }

    @Test
    void lruName_isFrozenAtSnapshotTime_notLiveReference() {
        Lru lru = firstLru();
        Interface iface = firstInterfaceForLru(lru);
        originalLruName = lru.getName();
        renamedLruId = lru.getId();

        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName("AI-185 LRU Isolation Procedure");
        request.setDescription("lru rename isolation check");
        request.setSteps(List.of(buildStepRequest(lru, iface, "behaviour", 1)));

        TestProcedureResponse created = testProcedureService.save(request);
        createdProcedureIds.add(created.getId());

        ProcedureHistoryResponse beforeRename = testProcedureService.getRevision(created.getId(), 1);
        assertEquals(originalLruName, beforeRename.getLruName());

        // Rename the LIVE LRU directly, bypassing the service layer entirely.
        Lru lruEntity = lruRepository.findById(lru.getId()).orElseThrow();
        lruEntity.setName(originalLruName + " (renamed)");
        lruRepository.save(lruEntity);

        ProcedureHistoryResponse afterRename = testProcedureService.getRevision(created.getId(), 1);
        assertEquals(originalLruName, afterRename.getLruName(),
                "procedure_history.lru_name must stay frozen at the value captured when the "
                        + "revision was written, even after the live LRU is renamed -- a diff "
                        + "engine must not need to re-query the lru table to know this");
        assertEquals(lru.getId(), afterRename.getLruId(),
                "lru_id itself is unaffected by rename, confirming this is purely a display-name staleness check");
    }

    @Test
    void stepReferenceNames_areFrozenAtSnapshotTime_notLiveReference() throws Exception {
        Lru lru = firstLru();
        Interface iface = firstInterfaceForLru(lru);
        originalInterfaceName = iface.getName();
        renamedInterfaceId = iface.getId();

        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName("AI-185 Interface Isolation Procedure");
        request.setDescription("interface rename isolation check");
        request.setSteps(List.of(buildStepRequest(lru, iface, "behaviour", 1)));

        TestProcedureResponse created = testProcedureService.save(request);
        createdProcedureIds.add(created.getId());

        // Rename the LIVE Interface directly, bypassing the service layer entirely.
        Interface ifaceEntity = interfaceRepository.findById(iface.getId()).orElseThrow();
        ifaceEntity.setName(originalInterfaceName + " (renamed)");
        interfaceRepository.save(ifaceEntity);

        ProcedureHistoryResponse revision = testProcedureService.getRevision(created.getId(), 1);
        JsonNode step = objectMapper.readTree(revision.getStepsSnapshot()).get(0);

        assertEquals(originalInterfaceName, step.get("interfaceName").asText(),
                "steps_snapshot.interfaceName must stay frozen at snapshot time, even after the "
                        + "live Interface is renamed -- this pattern already worked correctly "
                        + "before AI-185 and is verified here alongside the lru_name fix");
    }
}
