package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.*;
import com.adtech.iaterap.backend.entity.TestBlock;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.TestBlockRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * AI-179 [T7-A1]: organisation-wide reusable Test Block library.
 *
 * <p>{@link #insertIntoProcedure} deliberately delegates the actual
 * TestStep write to {@link TestProcedureService#update}, rather than
 * persisting the new TestStep directly, so a block-insert still goes
 * through the existing add/edit/reorder/remove diffing and still
 * produces a new immutable procedure_history revision. A block insert
 * is a procedure content change like any other and must be versioned
 * identically -- duplicating that logic here would silently break the
 * write-through invariant AI-176/AI-177 verified.</p>
 */
@Service
public class TestBlockService {

    private final TestBlockRepository testBlockRepository;
    private final TestStepRepository testStepRepository;
    private final TestProcedureService testProcedureService;

    public TestBlockService(
            TestBlockRepository testBlockRepository,
            TestStepRepository testStepRepository,
            TestProcedureService testProcedureService
    ) {
        this.testBlockRepository = testBlockRepository;
        this.testStepRepository = testStepRepository;
        this.testProcedureService = testProcedureService;
    }

    public List<TestBlockResponse> getAll() {
        return testBlockRepository.findAll().stream()
                .map(TestBlockResponse::fromEntity)
                .toList();
    }

    public TestBlockResponse getById(Long id) {
        return TestBlockResponse.fromEntity(requireBlock(id));
    }

    /** "create-from-step": promotes an existing TestStep's reusable content into the shared library. */
    @Transactional
    public TestBlockResponse createFromStep(Long testStepId, CreateTestBlockFromStepRequest request) {
        TestStep step = testStepRepository.findById(testStepId)
                .orElseThrow(() -> new NoSuchElementException("Test step not found: " + testStepId));

        if (testBlockRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalStateException("A Test Block named '" + request.getName() + "' already exists");
        }

        TestBlock block = new TestBlock(request.getName(), request.getDescription());
        block.setExpectedBehaviour(step.getExpectedBehaviour());
        block.setExpectedRange(step.getExpectedRange());
        block.setCalibrationRequirement(step.getCalibrationRequirement());
        block.setSafetyNotes(step.getSafetyNotes());
        block.setAcceptanceCriteria(step.getAcceptanceCriteria());
        block.setPreconditions(step.getPreconditions());
        block.setDependencies(step.getDependencies());
        block.setPostConditions(step.getPostConditions());
        block.setPassFailCriteria(step.getPassFailCriteria());
        block.setExpectedObservation(step.getExpectedObservation());
        block.setEngineerRemarks(step.getEngineerRemarks());
        block.setUsageCount(0);

        block = testBlockRepository.save(block);
        return TestBlockResponse.fromEntity(block);
    }

    /** "insert-into-procedure": reuses a shared block's content as a new TestStep in the target procedure. */
    @Transactional
    public TestProcedureResponse insertIntoProcedure(Long blockId, InsertTestBlockRequest request) {
        TestBlock block = requireBlock(blockId);

        if (block.getExpectedBehaviour() == null) {
            throw new IllegalStateException(
                    "Test Block " + blockId + " has no reusable content and cannot be inserted "
                            + "(likely a legacy AI-153 seeded shell row)");
        }

        TestProcedureResponse procedure = testProcedureService.getById(request.getTestProcedureId());

        List<TestStepRequest> steps = new ArrayList<>();
        for (TestStepResponse existingStep : procedure.getSteps()) {
            steps.add(toStepRequest(existingStep));
        }

        TestStepRequest newStep = new TestStepRequest();
        newStep.setInterfaceId(request.getInterfaceId());
        newStep.setConnectorId(request.getConnectorId());
        newStep.setSignalLibraryId(request.getSignalLibraryId());
        newStep.setProtocolTagId(request.getProtocolTagId());
        newStep.setFaultCategoryId(request.getFaultCategoryId());
        newStep.setExpectedBehaviour(block.getExpectedBehaviour());
        newStep.setExpectedRange(block.getExpectedRange());
        newStep.setCalibrationRequirement(block.getCalibrationRequirement());
        newStep.setSafetyNotes(block.getSafetyNotes());
        newStep.setAcceptanceCriteria(block.getAcceptanceCriteria());
        newStep.setPreconditions(block.getPreconditions());
        newStep.setDependencies(block.getDependencies());
        newStep.setExecutionOrder(steps.size() + 1);
        newStep.setPostConditions(block.getPostConditions());
        newStep.setPassFailCriteria(block.getPassFailCriteria());
        newStep.setExpectedObservation(block.getExpectedObservation());
        newStep.setEngineerRemarks(block.getEngineerRemarks());
        steps.add(newStep);

        TestProcedureRequest procedureRequest = new TestProcedureRequest();
        procedureRequest.setLruId(procedure.getLruId());
        procedureRequest.setName(procedure.getName());
        procedureRequest.setDescription(procedure.getDescription());
        procedureRequest.setSteps(steps);

        TestProcedureResponse updated = testProcedureService.update(request.getTestProcedureId(), procedureRequest);

        block.setUsageCount(block.getUsageCount() + 1);
        testBlockRepository.save(block);

        return updated;
    }

    private TestBlock requireBlock(Long id) {
        return testBlockRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Test block not found: " + id));
    }

    private TestStepRequest toStepRequest(TestStepResponse s) {
        TestStepRequest r = new TestStepRequest();
        r.setId(s.getId());
        r.setInterfaceId(s.getInterfaceId());
        r.setConnectorId(s.getConnectorId());
        r.setSignalLibraryId(s.getSignalLibraryId());
        r.setProtocolTagId(s.getProtocolTagId());
        r.setFaultCategoryId(s.getFaultCategoryId());
        r.setExpectedBehaviour(s.getExpectedBehaviour());
        r.setExpectedRange(s.getExpectedRange());
        r.setCalibrationRequirement(s.getCalibrationRequirement());
        r.setSafetyNotes(s.getSafetyNotes());
        r.setAcceptanceCriteria(s.getAcceptanceCriteria());
        r.setPreconditions(s.getPreconditions());
        r.setDependencies(s.getDependencies());
        r.setExecutionOrder(s.getExecutionOrder());
        r.setPostConditions(s.getPostConditions());
        r.setPassFailCriteria(s.getPassFailCriteria());
        r.setExpectedObservation(s.getExpectedObservation());
        r.setEngineerRemarks(s.getEngineerRemarks());
        return r;
    }
}