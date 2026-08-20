package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.*;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI-180 [T7-QA]: Test Block reuse isolation tests.
 *
 * <p>There is currently no "edit a Test Block" endpoint (AI-179 only built
 * create-from-step and insert-into-procedure), so these tests mutate the
 * TestBlock and source TestStep directly via their repositories. This is
 * actually the more honest way to prove the data-model invariant under
 * test: a TestStep created via {@link TestBlockService#insertIntoProcedure}
 * must store a copied snapshot of the block's content, not a live
 * reference to it, and a TestBlock created via
 * {@link TestBlockService#createFromStep} must likewise be a snapshot of
 * the source step, not a live reference back to it -- regardless of
 * whatever edit UI/endpoint gets built later.</p>
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class TestBlockReuseIsolationTest {

    @Autowired private TestBlockService testBlockService;
    @Autowired private TestProcedureService testProcedureService;
    @Autowired private TestBlockRepository testBlockRepository;
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
    private final List<Long> createdBlockIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long procedureId : createdProcedureIds) {
            jdbcTemplate.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", procedureId);
            testStepRepository.deleteAll(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));
            testProcedureRepository.deleteById(procedureId);
        }
        createdProcedureIds.clear();

        for (Long blockId : createdBlockIds) {
            testBlockRepository.deleteById(blockId);
        }
        createdBlockIds.clear();
    }

    // ---- fixtures, matching TwoRevisionRetrievabilityTest's pattern ----

    private Lru firstLru() {
        return lruRepository.findAll().get(0);
    }

    private TestStepRequest buildStepRequest(String behaviour, int order) {
        Lru lru = firstLru();
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
        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(firstLru().getId());
        request.setName(name);
        request.setDescription("AI-180 reuse isolation fixture");
        request.setSteps(List.of(buildStepRequest(behaviour, 1)));
        return request;
    }

    private InsertTestBlockRequest buildInsertRequest(Long targetProcedureId) {
        Lru lru = firstLru();
        Interface iface = interfaceRepository.findAll().stream()
                .filter(i -> i.getLru().getId().equals(lru.getId()))
                .findFirst()
                .orElseThrow();

        InsertTestBlockRequest request = new InsertTestBlockRequest();
        request.setTestProcedureId(targetProcedureId);
        request.setInterfaceId(iface.getId());
        request.setConnectorId(connectorRepository.findByLruId(lru.getId()).get(0).getId());
        request.setSignalLibraryId(signalLibraryRepository.findByLruId(lru.getId()).get(0).getId());
        request.setProtocolTagId(protocolTagRepository.findByLruId(lru.getId()).get(0).getId());
        request.setFaultCategoryId(faultTemplateRepository.findByLruId(lru.getId()).get(0).getId());
        return request;
    }

    // ---- tests ----

    @Test
    void createFromStep_copiesStepContentAsSnapshot_notLiveReferenceToStep() {
        TestProcedureResponse procedure = testProcedureService.save(
                buildProcedureRequest("AI-180 Source Procedure A", "original step behaviour"));
        createdProcedureIds.add(procedure.getId());
        Long sourceStepId = procedure.getSteps().get(0).getId();

        CreateTestBlockFromStepRequest createRequest = new CreateTestBlockFromStepRequest();
        createRequest.setName("AI-180 Isolation Block A");
        createRequest.setDescription("snapshot isolation check");

        TestBlockResponse block = testBlockService.createFromStep(sourceStepId, createRequest);
        createdBlockIds.add(block.getId());

        assertEquals("original step behaviour", block.getExpectedBehaviour(),
                "Block should be created with the source step's content");

        // Mutate the ORIGINAL step directly, bypassing the service layer entirely.
        TestStep sourceStep = testStepRepository.findById(sourceStepId).orElseThrow();
        sourceStep.setExpectedBehaviour("mutated after block was created");
        testStepRepository.save(sourceStep);

        TestBlockResponse blockAfterStepMutation = testBlockService.getById(block.getId());
        assertEquals("original step behaviour", blockAfterStepMutation.getExpectedBehaviour(),
                "Block content must not change when the original source step is mutated afterward -- "
                        + "createFromStep must copy content, not hold a live reference to the step");
    }

    @Test
    void insertIntoProcedure_copiesBlockContentAsSnapshot_notLiveReferenceToBlock() {
        // Build a block from a throwaway source step.
        TestProcedureResponse sourceProcedure = testProcedureService.save(
                buildProcedureRequest("AI-180 Source Procedure B", "block seed behaviour"));
        createdProcedureIds.add(sourceProcedure.getId());
        Long sourceStepId = sourceProcedure.getSteps().get(0).getId();

        CreateTestBlockFromStepRequest createRequest = new CreateTestBlockFromStepRequest();
        createRequest.setName("AI-180 Isolation Block B");
        createRequest.setDescription("insert isolation check");
        TestBlockResponse block = testBlockService.createFromStep(sourceStepId, createRequest);
        createdBlockIds.add(block.getId());

        // Insert the block into a separate target procedure.
        TestProcedureResponse targetProcedure = testProcedureService.save(
                buildProcedureRequest("AI-180 Target Procedure B", "target's own first step"));
        createdProcedureIds.add(targetProcedure.getId());

        TestProcedureResponse afterInsert = testBlockService.insertIntoProcedure(
                block.getId(), buildInsertRequest(targetProcedure.getId()));

        TestStepResponse insertedStep = afterInsert.getSteps().stream()
                .filter(s -> "block seed behaviour".equals(s.getExpectedBehaviour()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Inserted step not found in target procedure"));
        assertNotNull(insertedStep.getId());

        // Mutate the BLOCK directly, bypassing the service layer entirely.
        TestBlock blockEntity = testBlockRepository.findById(block.getId()).orElseThrow();
        blockEntity.setExpectedBehaviour("mutated after insert");
        testBlockRepository.save(blockEntity);

        TestProcedureResponse targetAfterBlockMutation = testProcedureService.getById(targetProcedure.getId());
        boolean stillHasOriginalContent = targetAfterBlockMutation.getSteps().stream()
                .anyMatch(s -> "block seed behaviour".equals(s.getExpectedBehaviour()));
        boolean anyStepPickedUpMutation = targetAfterBlockMutation.getSteps().stream()
                .anyMatch(s -> "mutated after insert".equals(s.getExpectedBehaviour()));

        assertTrue(stillHasOriginalContent,
                "The already-inserted TestStep must retain its original content when the source "
                        + "TestBlock is mutated afterward -- insertIntoProcedure must copy content, "
                        + "not hold a live reference to the block");
        assertFalse(anyStepPickedUpMutation,
                "No step in the target procedure should reflect the block's post-insert mutation");
    }

    @Test
    void insertIntoProcedure_incrementsUsageCountPerInsert() {
        TestProcedureResponse sourceProcedure = testProcedureService.save(
                buildProcedureRequest("AI-180 Source Procedure C", "usage count seed"));
        createdProcedureIds.add(sourceProcedure.getId());
        Long sourceStepId = sourceProcedure.getSteps().get(0).getId();

        CreateTestBlockFromStepRequest createRequest = new CreateTestBlockFromStepRequest();
        createRequest.setName("AI-180 Isolation Block C");
        TestBlockResponse block = testBlockService.createFromStep(sourceStepId, createRequest);
        createdBlockIds.add(block.getId());
        assertEquals(0, block.getUsageCount());

        TestProcedureResponse targetOne = testProcedureService.save(
                buildProcedureRequest("AI-180 Target Procedure C1", "target one seed"));
        createdProcedureIds.add(targetOne.getId());
        testBlockService.insertIntoProcedure(block.getId(), buildInsertRequest(targetOne.getId()));

        assertEquals(1, testBlockService.getById(block.getId()).getUsageCount(),
                "usageCount should increment after the first insert");

        TestProcedureResponse targetTwo = testProcedureService.save(
                buildProcedureRequest("AI-180 Target Procedure C2", "target two seed"));
        createdProcedureIds.add(targetTwo.getId());
        testBlockService.insertIntoProcedure(block.getId(), buildInsertRequest(targetTwo.getId()));

        assertEquals(2, testBlockService.getById(block.getId()).getUsageCount(),
                "usageCount should increment again after a second, independent insert");
    }

    @Test
    void insertIntoProcedure_rejectsContentlessLegacyBlock() {
        // Simulates an AI-153-era seeded shell row: name/description only, no reusable content.
        TestBlock legacyShell = new TestBlock("AI-180 Legacy Shell Block", "pre-AI-179 shell row");
        legacyShell = testBlockRepository.save(legacyShell);
        createdBlockIds.add(legacyShell.getId());

        TestProcedureResponse targetProcedure = testProcedureService.save(
                buildProcedureRequest("AI-180 Target Procedure D", "target seed"));
        createdProcedureIds.add(targetProcedure.getId());

        Long legacyBlockId = legacyShell.getId();
        assertThrows(IllegalStateException.class,
                () -> testBlockService.insertIntoProcedure(legacyBlockId, buildInsertRequest(targetProcedure.getId())),
                "A content-less legacy Test Block must be rejected, not inserted as blank content");
    }

    @Test
    void createFromStep_rejectsDuplicateBlockName() {
        TestProcedureResponse procedureOne = testProcedureService.save(
                buildProcedureRequest("AI-180 Source Procedure E1", "first behaviour"));
        createdProcedureIds.add(procedureOne.getId());
        Long stepOneId = procedureOne.getSteps().get(0).getId();

        CreateTestBlockFromStepRequest firstRequest = new CreateTestBlockFromStepRequest();
        firstRequest.setName("AI-180 Duplicate Name Block");
        TestBlockResponse firstBlock = testBlockService.createFromStep(stepOneId, firstRequest);
        createdBlockIds.add(firstBlock.getId());

        TestProcedureResponse procedureTwo = testProcedureService.save(
                buildProcedureRequest("AI-180 Source Procedure E2", "second behaviour"));
        createdProcedureIds.add(procedureTwo.getId());
        Long stepTwoId = procedureTwo.getSteps().get(0).getId();

        CreateTestBlockFromStepRequest secondRequest = new CreateTestBlockFromStepRequest();
        secondRequest.setName("AI-180 Duplicate Name Block");

        assertThrows(IllegalStateException.class,
                () -> testBlockService.createFromStep(stepTwoId, secondRequest),
                "A second Test Block with the same name must be rejected");
    }
}
