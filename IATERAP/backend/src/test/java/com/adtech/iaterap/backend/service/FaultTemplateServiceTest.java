package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaultTemplateServiceTest {

    @Mock
    private FaultTemplateRepository repository;

    @Mock
    private FmeaBranchMappingRepository fmeaBranchMappingRepository;

    private FaultTemplateService service;

    @BeforeEach
    void setUp() {
        service = new FaultTemplateService(repository, fmeaBranchMappingRepository);
    }

    @Test
    void shouldReturnAllFaultTemplates() {
        FaultTemplate template = new FaultTemplate(
                "Loss of Signal",
                "FT-001",
                "Fault template for loss of signal on the flight control unit.",
                "CRITICAL",
                new Lru("Flight Control Unit")
        );

        when(repository.findAll()).thenReturn(List.of(template));

        List<FaultTemplate> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("Loss of Signal", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoFaultTemplatesExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<FaultTemplate> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnFaultTemplatesByLruId() {
        FaultTemplate template = new FaultTemplate(
                "Power Fluctuation",
                "FT-002",
                "Fault template for power fluctuation on the PDU.",
                "WARNING",
                new Lru("Power Distribution Unit")
        );

        when(repository.findByLruId(2L)).thenReturn(List.of(template));

        List<FaultTemplate> result = service.getByLruId(2L);

        assertEquals(1, result.size());
        assertEquals("FT-002", result.get(0).getFaultCode());

        verify(repository, times(1)).findByLruId(2L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnFaultTemplateById() {
        FaultTemplate template = new FaultTemplate(
                "Loss of Signal",
                "FT-001",
                "Description",
                "CRITICAL",
                new Lru("Flight Control Unit")
        );

        when(repository.findById(1L)).thenReturn(Optional.of(template));

        FaultTemplate result = service.getById(1L);

        assertEquals("Loss of Signal", result.getName());

        verify(repository, times(1)).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowWhenFaultTemplateDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.getById(99L)
        );

        assertEquals("Fault template not found: 99", exception.getMessage());

        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveFaultTemplateSuccessfully() {
        FaultTemplate template = new FaultTemplate(
                "Loss of Signal",
                "FT-001",
                "Description",
                "CRITICAL",
                new Lru("Flight Control Unit")
        );

        when(repository.save(template)).thenReturn(template);

        FaultTemplate result = service.save(template);

        assertNotNull(result);
        assertEquals("FT-001", result.getFaultCode());

        verify(repository, times(1)).save(template);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateFaultTemplateSuccessfully() {
        FaultTemplate existing = new FaultTemplate(
                "Old Name",
                "FT-001",
                "Old description",
                "WARNING",
                new Lru("Flight Control Unit")
        );

        FaultTemplate incoming = new FaultTemplate(
                "Updated Name",
                "FT-001-A",
                "Updated description",
                "CRITICAL",
                new Lru("Power Distribution Unit")
        );

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FaultTemplate result = service.update(1L, incoming);

        assertEquals("Updated Name", result.getName());
        assertEquals("FT-001-A", result.getFaultCode());
        assertEquals("CRITICAL", result.getSeverity());

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteFaultTemplateAndCascadeFmeaBranchMappings() {
        service.delete(1L);

        verify(fmeaBranchMappingRepository, times(1)).deleteByFaultTemplateId(1L);
        verify(repository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(fmeaBranchMappingRepository);
        verifyNoMoreInteractions(repository);
    }
}
