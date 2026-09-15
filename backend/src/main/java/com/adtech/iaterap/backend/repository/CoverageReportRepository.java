package com.adtech.iaterap.backend.repository;
import com.adtech.iaterap.backend.entity.CoverageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CoverageReportRepository extends JpaRepository<CoverageReport, Long> { Optional<CoverageReport> findByTestProcedureIdAndProcedureVersionNumberAndReportVersion(Long procedureId, Integer version, Integer reportVersion); }
