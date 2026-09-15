package com.adtech.iaterap.backend.repository;
import com.adtech.iaterap.backend.entity.ProcedureSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ProcedureSnapshotRepository extends JpaRepository<ProcedureSnapshot, Long> { Optional<ProcedureSnapshot> findByTestProcedureIdAndProcedureVersionNumber(Long procedureId, Integer version); }
