package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.entity.FmeaBranchMapping;
import com.adtech.iaterap.backend.entity.Lru;
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
class FmeaBranchMappingServiceTest {

    @Mock
    private FmeaBranchMappingRepository repository;

    private FmeaBranchMappingService service;

    @BeforeEach
    void setUp() {
        service = new FmeaBranchMappingService(repository);
    }

    private FaultTemplate sampleFaultTemplate() {
        return new FaultTemplate(
                "Loss of Signal",
                "FT-001",
                "Description",
                "CRITICAL",
                new Lru("Flight Control Unit")
        );
    }

    @Test
    void shouldReturnAllFmeaBranchMappings() {
        FmeaBranchMapping mapping = new FmeaBranchMapping(
                "3.2.1",
                "Loss of flight control command output",
                "Signal wiring degradation",
                "Built-in test monitoring",
                "Replace wiring harness",
                80,
                new Lru("Flight Control Unit"),
                sampleFaultTemplate()
        );

        when(repository.findAll()).thenReturn(List.of(mapping));

        List<FmeaBranchMapping> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("3.2.1", result.get(0).getFmeaBranchCode());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoFmeaBranchMappingsExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<FmeaBranchMapping> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnFmeaBranchMappingsByLruId() {
        FmeaBranchMapping mapping = new FmeaBranchMapping(
                "4.1.0",
                "Power distribution overload",
                "Excess current draw",
                "Current sensor",
                "Add circuit breaker",
                60,
                new Lru("Power Distribution Unit"),
                sampleFaultTemplate()
        );

        when(repository.findByLruId(2L)).thenReturn(List.of(mapping));

        List<FmeaBranchMapping> result = service.getByLruId(2L);

        assertEquals(1, result.size());
        assertEquals("4.1.0", result.get(0).getFmeaBranchCode());

        verify(repository, times(1)).findByLruId(2L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnFmeaBranchMappingsByFaultTemplateId() {
        FmeaBranchMapping mapping = new FmeaBranchMapping(
                "3.2.1",
                "Loss of flight control command output",
                "Signal wiring degradation",
                "Built-in test monitoring",
                "Replace wiring harness",
                80,
                new Lru("Flight Control Unit"),
                sampleFaultTemplate()
        );

        when(repository.findByFaultTemplateId(5L)).thenReturn(List.of(mapping));

        List<FmeaBranchMapping> result = service.getByFaultTemplateId(5L);

        assertEquals(1, result.size());

        verify(repository, times(1)).findByFaultTemplateId(5L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnFmeaBranchMappingById() {
        FmeaBranchMapping mapping = new FmeaBranchMapping(
                "3.2.1",
                "Loss of flight control command output",
                "Signal wiring degradation",
                "Built-in test monitoring",
                "Replace wiring harness",
                80,
                new Lru("Flight Control Unit"),
                sampleFaultTemplate()
        );

        when(repository.findById(1L)).thenReturn(Optional.of(mapping));

        FmeaBranchMapping result = service.getById(1L);

        assertEquals("3.2.1", result.getFmeaBranchCode());

        verify(repository, times(1)).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowWhenFmeaBranchMappingDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.getById(99L)
        );

        assertEquals("FMEA branch mapping not found: 99", exception.getMessage());

        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveFmeaBranchMappingSuccessfully() {
        FmeaBranchMapping mapping = new FmeaBranchMapping(
                "3.2.1",
                "Loss of flight control command output",
                "Signal wiring degradation",
                "Built-in test monitoring",
                "Replace wiring harness",
                80,
                new Lru("Flight Control Unit"),
                sampleFaultTemplate()
        );

        when(repository.save(mapping)).thenReturn(mapping);

        FmeaBranchMapping result = service.save(mapping);

        assertNotNull(result);
        assertEquals(80, result.getRiskPriorityNumber());

        verify(repository, times(1)).save(mapping);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateFmeaBranchMappingSuccessfully() {
        FmeaBranchMapping existing = new FmeaBranchMapping(
                "3.2.1",
                "Old effect",
                "Old cause",
                "Old detection",
                "Old action",
                40,
                new Lru("Flight Control Unit"),
                sampleFaultTemplate()
        );

        FmeaBranchMapping incoming = new FmeaBranchMapping(
                "3.2.2",
                "Updated effect",
                "Updated cause",
                "Updated detection",
                "Updated action",
                90,
                new Lru("Power Distribution Unit"),
                sampleFaultTemplate()
        );

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FmeaBranchMapping result = service.update(1L, incoming);

        assertEquals("3.2.2", result.getFmeaBranchCode());
        assertEquals("Updated effect", result.getFailureEffect());
        assertEquals(90, result.getRiskPriorityNumber());

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteFmeaBranchMappingById() {
        service.delete(1L);

        verify(repository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(repository);
    }
}
