package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TestProcedureService {

    private final TestProcedureRepository repository;
    private final TestStepRepository testStepRepository;

    public TestProcedureService(
            TestProcedureRepository repository,
            TestStepRepository testStepRepository
    ) {
        this.repository = repository;
        this.testStepRepository = testStepRepository;
    }

    public List<TestProcedure> getAll() {
        return repository.findAll();
    }

    public List<TestProcedure> getByLruId(Long lruId) {
        return repository.findByLruId(lruId);
    }

    public List<TestProcedure> getByApprovalStatus(String approvalStatus) {
        return repository.findByApprovalStatus(approvalStatus);
    }

    public TestProcedure getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Test procedure not found: " + id
                ));
    }

    public TestProcedure create(TestProcedure procedure) {
        procedure.setApprovalStatus("DRAFT");
        procedure.setVersionNumber(1);
        return repository.save(procedure);
    }

    public TestProcedure update(Long id, TestProcedure incoming) {
        TestProcedure existing = getById(id);
        existing.setLru(incoming.getLru());
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        testStepRepository.deleteAll(
                testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(id)
        );
        repository.deleteById(id);
    }
}
