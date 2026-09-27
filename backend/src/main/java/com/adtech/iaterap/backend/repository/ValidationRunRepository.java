package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.ValidationRun;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, Long> {
    Optional<ValidationRun> findByProcedureHistoryIdAndSourceSha256(
            Long procedureHistoryId, String sourceSha256);

    List<ValidationRun> findByProcedureHistoryIdOrderByUploadedAtDesc(Long procedureHistoryId);
}
