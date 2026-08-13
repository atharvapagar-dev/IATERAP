package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestStepRepository extends JpaRepository<TestStep, Long> {

    List<TestStep> findByTestProcedureIdOrderByExecutionOrderAsc(Long testProcedureId);
}
