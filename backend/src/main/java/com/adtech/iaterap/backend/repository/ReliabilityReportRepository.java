package com.adtech.iaterap.backend.repository;
import com.adtech.iaterap.backend.entity.ReliabilityReport;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReliabilityReportRepository extends JpaRepository<ReliabilityReport, Long> {
    List<ReliabilityReport> findByPeriodStartOrderByLruNameAsc(LocalDate periodStart);
    void deleteByPeriodStart(LocalDate periodStart);
}
