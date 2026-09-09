package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.ValidationReading;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationReadingRepository extends JpaRepository<ValidationReading, Long> {
    List<ValidationReading> findByValidationRunIdOrderBySourceRowNumberAsc(Long validationRunId);
}
