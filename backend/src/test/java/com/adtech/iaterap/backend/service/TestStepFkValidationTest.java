package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI-163 [T5-A3] - FK-validation tests for Test Step fields.
 * Validates that TestStep FK references (Interface, Connector, SignalLibrary,
 * ProtocolTag, FaultTemplate, TestProcedure) resolve to real, persisted entities.
 * Runs against real Postgres under the synthetic-data profile.
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class TestStepFkValidationTest {

    @Autowired
    private TestStepRepository testStepRepository;

    @Autowired
    private InterfaceRepository interfaceRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private SignalLibraryRepository signalLibraryRepository;

    @Autowired
    private ProtocolTagRepository protocolTagRepository;

    @Autowired
    private FaultTemplateRepository faultTemplateRepository;

    @Autowired
    private TestProcedureRepository testProcedureRepository;

    @Autowired
    private LruRepository lruRepository;

    @Test
    void testStepCanBePersistedWithValidInterfaceAndProcedure() {
        Interface iface = interfaceRepository.findAll().get(0);
        Lru lru = lruRepository.findAll().get(0);

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName("Test Procedure - FK Validation (Synthetic)");
        procedure.setApprovalStatus("DRAFT");
        procedure = testProcedureRepository.save(procedure);

        TestStep step = new TestStep();
        step.setInterfaceEntity(iface);
        step.setTestProcedure(procedure);
        step.setExpectedBehaviour("Signal present on interface");
        step.setAcceptanceCriteria("Signal within nominal range");
        step.setPassFailCriteria("Pass if within range, fail otherwise");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestStep saved = testStepRepository.save(step);
        assertNotNull(saved.getId(), "TestStep should have an ID after save");
        assertEquals(iface.getId(), saved.getInterfaceEntity().getId());
        assertEquals(procedure.getId(), saved.getTestProcedure().getId());

        testStepRepository.delete(saved);
        testProcedureRepository.delete(procedure);
    }

    @Test
    void testStepCanReferenceConnector() {
        Connector connector = connectorRepository.findAll().get(0);
        Lru lru = lruRepository.findAll().get(0);

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName("Test Procedure - Connector FK (Synthetic)");
        procedure.setApprovalStatus("DRAFT");
        procedure = testProcedureRepository.save(procedure);

        TestStep step = new TestStep();
        step.setConnector(connector);
        step.setTestProcedure(procedure);
        step.setExpectedBehaviour("Connector continuity verified");
        step.setAcceptanceCriteria("No open circuit");
        step.setPassFailCriteria("Pass if continuity present");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestStep saved = testStepRepository.save(step);
        assertEquals(connector.getId(), saved.getConnector().getId());

        testStepRepository.delete(saved);
        testProcedureRepository.delete(procedure);
    }

    @Test
    void testStepCanReferenceSignalLibrary() {
        SignalLibrary signal = signalLibraryRepository.findAll().get(0);
        Lru lru = lruRepository.findAll().get(0);

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName("Test Procedure - Signal FK (Synthetic)");
        procedure.setApprovalStatus("DRAFT");
        procedure = testProcedureRepository.save(procedure);

        TestStep step = new TestStep();
        step.setSignalLibrary(signal);
        step.setTestProcedure(procedure);
        step.setExpectedBehaviour("Signal observed on channel");
        step.setAcceptanceCriteria("Signal within spec");
        step.setPassFailCriteria("Pass if in spec");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestStep saved = testStepRepository.save(step);
        assertEquals(signal.getId(), saved.getSignalLibrary().getId());

        testStepRepository.delete(saved);
        testProcedureRepository.delete(procedure);
    }

    @Test
    void testStepCanReferenceProtocolTag() {
        ProtocolTag tag = protocolTagRepository.findAll().get(0);
        Lru lru = lruRepository.findAll().get(0);

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName("Test Procedure - Protocol FK (Synthetic)");
        procedure.setApprovalStatus("DRAFT");
        procedure = testProcedureRepository.save(procedure);

        TestStep step = new TestStep();
        step.setProtocolTag(tag);
        step.setTestProcedure(procedure);
        step.setExpectedBehaviour("Protocol handshake verified");
        step.setAcceptanceCriteria("Handshake completes within timeout");
        step.setPassFailCriteria("Pass if handshake completes");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestStep saved = testStepRepository.save(step);
        assertEquals(tag.getId(), saved.getProtocolTag().getId());

        testStepRepository.delete(saved);
        testProcedureRepository.delete(procedure);
    }

    @Test
    void testStepCanReferenceFaultTemplate() {
        FaultTemplate fault = faultTemplateRepository.findAll().get(0);
        Lru lru = lruRepository.findAll().get(0);

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName("Test Procedure - Fault FK (Synthetic)");
        procedure.setApprovalStatus("DRAFT");
        procedure = testProcedureRepository.save(procedure);

        TestStep step = new TestStep();
        step.setFaultCategory(fault);
        step.setTestProcedure(procedure);
        step.setExpectedBehaviour("Fault condition triggered and detected");
        step.setAcceptanceCriteria("Fault detected within 100ms");
        step.setPassFailCriteria("Pass if fault detected");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestStep saved = testStepRepository.save(step);
        assertEquals(fault.getId(), saved.getFaultCategory().getId());

        testStepRepository.delete(saved);
        testProcedureRepository.delete(procedure);
    }

    @Test
    void testStepApprovalStatusMustBeValidValue() {
        List<TestStep> allSteps = testStepRepository.findAll();
        for (TestStep step : allSteps) {
            assertTrue(
                List.of("DRAFT", "SUBMITTED", "APPROVED", "REJECTED")
                    .contains(step.getApprovalStatus()),
                "TestStep id=" + step.getId() + " has invalid approval_status: " + step.getApprovalStatus()
            );
        }
    }

    @Test
    void testStepRejectsNonExistentInterfaceReference() {
        Lru lru = lruRepository.findAll().get(0);

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName("Test Procedure - Invalid Interface FK (Synthetic)");
        procedure.setApprovalStatus("DRAFT");
        procedure = testProcedureRepository.save(procedure);

        Interface bogusInterface = interfaceRepository.getReferenceById(999999L);

        TestStep step = new TestStep();
        step.setInterfaceEntity(bogusInterface);
        step.setTestProcedure(procedure);
        step.setExpectedBehaviour("Should never persist");
        step.setAcceptanceCriteria("Should never persist");
        step.setPassFailCriteria("Should never persist");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestProcedure finalProcedure = procedure;
        assertThrows(
            DataIntegrityViolationException.class,
            () -> testStepRepository.save(step),
            "Saving a TestStep with a non-existent interface_id should violate the FK constraint"
        );

        testProcedureRepository.delete(finalProcedure);
    }

    @Test
    void testStepRejectsNonExistentTestProcedureReference() {
        Interface iface = interfaceRepository.findAll().get(0);
        TestProcedure bogusProcedure = testProcedureRepository.getReferenceById(999999L);

        TestStep step = new TestStep();
        step.setInterfaceEntity(iface);
        step.setTestProcedure(bogusProcedure);
        step.setExpectedBehaviour("Should never persist");
        step.setAcceptanceCriteria("Should never persist");
        step.setPassFailCriteria("Should never persist");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        assertThrows(
            DataIntegrityViolationException.class,
            () -> testStepRepository.save(step),
            "Saving a TestStep with a non-existent test_procedure_id should violate the FK constraint"
        );
    }

    @Test
    void testStepRejectsInterfaceFromDifferentLru() {
        List<Lru> lrus = lruRepository.findAll();
        Lru lruA = lrus.get(0);
        Lru lruB = lrus.get(1);

        Interface interfaceFromLruA = interfaceRepository.findAll().stream()
                .filter(i -> i.getLru().getId().equals(lruA.getId()))
                .findFirst()
                .orElseThrow();

        TestProcedure procedureUnderLruB = new TestProcedure();
        procedureUnderLruB.setLru(lruB);
        procedureUnderLruB.setName("Test Procedure - Cross-LRU Rejection (Synthetic)");
        procedureUnderLruB.setApprovalStatus("DRAFT");
        procedureUnderLruB = testProcedureRepository.save(procedureUnderLruB);

        TestStep step = new TestStep();
        step.setInterfaceEntity(interfaceFromLruA);
        step.setTestProcedure(procedureUnderLruB);
        step.setExpectedBehaviour("Should never persist - cross-LRU reference");
        step.setAcceptanceCriteria("Should never persist");
        step.setPassFailCriteria("Should never persist");
        step.setExecutionOrder(1);
        step.setApprovalStatus("DRAFT");

        TestProcedure finalProcedure = procedureUnderLruB;
        assertThrows(
            DataIntegrityViolationException.class,
            () -> testStepRepository.save(step),
            "A TestStep whose TestProcedure belongs to LRU B must not accept an Interface belonging to LRU A"
        );

        testProcedureRepository.delete(finalProcedure);
    }
}