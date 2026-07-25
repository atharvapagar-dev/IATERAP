package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameOrderByCreatedAtDesc(String entityName);

    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(
            String entityName,
            String entityId
    );

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
