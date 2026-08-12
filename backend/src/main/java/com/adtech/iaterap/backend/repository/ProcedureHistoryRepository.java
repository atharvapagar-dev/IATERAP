package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureHistoryRepository extends JpaRepository<ProcedureHistory, Long> {

    List<ProcedureHistory> findByTestProcedureIdOrderByVersionNumberAsc(Long testProcedureId);

    Optional<ProcedureHistory> findByTestProcedureIdAndVersionNumber(Long testProcedureId, Integer versionNumber);

    Optional<ProcedureHistory> findTopByTestProcedureIdOrderByVersionNumberDesc(Long testProcedureId);
}
