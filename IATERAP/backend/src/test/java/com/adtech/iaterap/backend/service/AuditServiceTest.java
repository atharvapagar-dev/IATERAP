package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.AuditLog;
import com.adtech.iaterap.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    private AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(repository);
    }

    @Test
    void shouldRecordAuditEntryWithAllFields() {
        AuditLog saved = new AuditLog(
                "admin",
                "CREATE",
                "FaultTemplate",
                "1",
                null,
                "{\"id\":1,\"name\":\"Loss of Signal\"}",
                "POST /api/fault-templates"
        );

        when(repository.save(any(AuditLog.class))).thenReturn(saved);

        AuditLog result = service.record(
                "admin",
                "CREATE",
                "FaultTemplate",
                "1",
                null,
                "{\"id\":1,\"name\":\"Loss of Signal\"}",
                "POST /api/fault-templates"
        );

        assertNotNull(result);
        assertEquals("admin", result.getActor());
        assertEquals("CREATE", result.getAction());
        assertEquals("FaultTemplate", result.getEntityName());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(repository);

        AuditLog captured = captor.getValue();
        assertEquals("1", captured.getEntityId());
        assertNull(captured.getBeforeState());
        assertEquals(
                "{\"id\":1,\"name\":\"Loss of Signal\"}",
                captured.getAfterState()
        );
    }

    @Test
    void shouldReturnAllAuditLogsOrderedByCreatedAtDescending() {
        AuditLog entry = new AuditLog(
                "admin",
                "UPDATE",
                "ProtocolTag",
                "2",
                "{\"value\":\"OLD\"}",
                "{\"value\":\"NEW\"}",
                "PUT /api/protocol-tags/2"
        );

        when(repository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(entry));

        List<AuditLog> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("UPDATE", result.get(0).getAction());

        verify(repository, times(1)).findAllByOrderByCreatedAtDesc();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoAuditLogsExistForEntity() {
        when(repository.findByEntityNameOrderByCreatedAtDesc("FaultTemplate"))
                .thenReturn(Collections.emptyList());

        List<AuditLog> result = service.getByEntityName("FaultTemplate");

        assertTrue(result.isEmpty());

        verify(repository, times(1))
                .findByEntityNameOrderByCreatedAtDesc("FaultTemplate");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnAuditLogsByEntityNameAndEntityId() {
        AuditLog entry = new AuditLog(
                "admin",
                "DELETE",
                "FmeaBranchMapping",
                "9",
                "{\"id\":9}",
                null,
                "DELETE /api/fmea-branch-mappings/9"
        );

        when(repository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                "FmeaBranchMapping",
                "9"
        )).thenReturn(List.of(entry));

        List<AuditLog> result = service.getByEntityNameAndEntityId(
                "FmeaBranchMapping",
                "9"
        );

        assertEquals(1, result.size());
        assertEquals("DELETE", result.get(0).getAction());
        assertNull(result.get(0).getAfterState());

        verify(repository, times(1))
                .findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                        "FmeaBranchMapping",
                        "9"
                );
        verifyNoMoreInteractions(repository);
    }
}
