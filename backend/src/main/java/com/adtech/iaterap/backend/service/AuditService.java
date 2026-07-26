package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.AuditLog;
import com.adtech.iaterap.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists a single write-through audit trail entry.
     * Runs in its own transaction so that an audit failure never
     * silently rolls back, and a rolled back business transaction
     * still leaves behind whatever it already audited.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(
            String actor,
            String action,
            String entityName,
            String entityId,
            String beforeState,
            String afterState,
            String metadata
    ) {
        AuditLog auditLog = new AuditLog(
                actor,
                action,
                entityName,
                entityId,
                beforeState,
                afterState,
                metadata
        );

        return repository.save(auditLog);
    }

    public List<AuditLog> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getByEntityName(String entityName) {
        return repository.findByEntityNameOrderByCreatedAtDesc(entityName);
    }

    public List<AuditLog> getByEntityNameAndEntityId(
            String entityName,
            String entityId
    ) {
        return repository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                entityName,
                entityId
        );
    }
}
