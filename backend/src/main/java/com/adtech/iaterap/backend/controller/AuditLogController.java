package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.AuditLog;
import com.adtech.iaterap.backend.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService service;

    public AuditLogController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditLog> getAll() {
        return service.getAll();
    }

    @GetMapping("/by-entity/{entityName}")
    public List<AuditLog> getByEntityName(@PathVariable String entityName) {
        return service.getByEntityName(entityName);
    }

    @GetMapping("/by-entity/{entityName}/{entityId}")
    public List<AuditLog> getByEntityNameAndEntityId(
            @PathVariable String entityName,
            @PathVariable String entityId
    ) {
        return service.getByEntityNameAndEntityId(entityName, entityId);
    }
}
