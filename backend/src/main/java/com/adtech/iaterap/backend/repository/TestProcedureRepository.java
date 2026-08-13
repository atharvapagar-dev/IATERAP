package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.TestProcedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestProcedureRepository extends JpaRepository<TestProcedure, Long> {
    List<TestProcedure> findByLruId(Long lruId);
    List<TestProcedure> findByApprovalStatus(String approvalStatus);
}
