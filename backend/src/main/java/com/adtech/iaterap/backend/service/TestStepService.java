package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TestStepService {

    private final TestStepRepository repository;
    private final TestProcedureRepository testProcedureRepository;

    public TestStepService(
            TestStepRepository repository,
            TestProcedureRepository testProcedureRepository
    ) {
        this.repository = repository;
        this.testProcedureRepository = testProcedureRepository;
    }

    public List<TestStep> getByProcedureId(Long procedureId) {
        return repository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId);
    }

    public TestStep getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Test step not found: " + id
                ));
    }

    @Transactional
    public TestStep addStep(Long procedureId, TestStep step) {
        TestProcedure procedure = testProcedureRepository.findById(procedureId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Test procedure not found: " + procedureId
                ));

        List<TestStep> existing = repository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId);
        int nextOrder = existing.isEmpty()
                ? 1
                : existing.get(existing.size() - 1).getExecutionOrder() + 1;

        step.setTestProcedure(procedure);
        step.setExecutionOrder(nextOrder);
        step.setApprovalStatus("DRAFT");
        return repository.save(step);
    }

    public TestStep updateStep(Long id, TestStep incoming) {
        TestStep existing = getById(id);
        existing.setInterfaceEntity(incoming.getInterfaceEntity());
        existing.setConnector(incoming.getConnector());
        existing.setSignalLibrary(incoming.getSignalLibrary());
        existing.setProtocolTag(incoming.getProtocolTag());
        existing.setFaultCategory(incoming.getFaultCategory());
        existing.setExpectedBehaviour(incoming.getExpectedBehaviour());
        existing.setExpectedRange(incoming.getExpectedRange());
        existing.setExpectedUnit(incoming.getExpectedUnit());
        existing.setCalibrationRequirement(incoming.getCalibrationRequirement());
        existing.setSafetyNotes(incoming.getSafetyNotes());
        existing.setAcceptanceCriteria(incoming.getAcceptanceCriteria());
        existing.setPreconditions(incoming.getPreconditions());
        existing.setDependencies(incoming.getDependencies());
        existing.setPostConditions(incoming.getPostConditions());
        existing.setPassFailCriteria(incoming.getPassFailCriteria());
        existing.setExpectedObservation(incoming.getExpectedObservation());
        existing.setEngineerRemarks(incoming.getEngineerRemarks());
        return repository.save(existing);
    }

    @Transactional
    public List<TestStep> reorderSteps(Long procedureId, List<Long> orderedStepIds) {
        List<TestStep> currentSteps = repository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId);

        if (currentSteps.size() != orderedStepIds.size()) {
            throw new IllegalArgumentException(
                    "Reorder list size (" + orderedStepIds.size() + ") does not match "
                    + "the procedure's current step count (" + currentSteps.size() + ")"
            );
        }

        for (int i = 0; i < orderedStepIds.size(); i++) {
            Long stepId = orderedStepIds.get(i);
            TestStep step = currentSteps.stream()
                    .filter(s -> s.getId().equals(stepId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Step id=" + stepId + " does not belong to procedure id=" + procedureId
                    ));
            step.setExecutionOrder(i + 1);
        }

        return repository.saveAll(currentSteps);
    }

    public void deleteStep(Long id) {
        getById(id);
        repository.deleteById(id);
    }
}
