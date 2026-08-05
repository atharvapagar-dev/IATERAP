package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.TestProcedure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestProcedureRepository extends JpaRepository<TestProcedure, Long> {
}