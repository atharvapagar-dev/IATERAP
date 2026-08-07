package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.SignalRangeRepository;
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
class SignalLibraryServiceTest {

    @Mock
    private SignalLibraryRepository repository;

    @Mock
    private SignalRangeRepository signalRangeRepository;

    private SignalLibraryService service;

    @BeforeEach
    void setUp() {
        service = new SignalLibraryService(repository, signalRangeRepository);
    }

    @Test
    void shouldReturnAllSignalLibraries() {
        SignalLibrary first = new SignalLibrary(
                "Engine Signals",
                "Core engine telemetry",
                "V",
                new Interface("CAN", new Lru("Interface Owner LRU")),
                new Lru("Flight Control Unit")
        );
        SignalLibrary second = new SignalLibrary(
                "Cabin Signals",
                "Cabin monitoring telemetry",
                "A",
                new Interface("ARINC 429", new Lru("Interface Owner LRU")),
                new Lru("Power Distribution Unit")
        );

        when(repository.findAll()).thenReturn(List.of(first, second));

        List<SignalLibrary> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("Engine Signals", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoSignalLibrariesExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<SignalLibrary> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveSignalLibrarySuccessfully() {
        SignalLibrary signalLibrary = new SignalLibrary(
                "Flight Control Signals",
                "Signals for flight control",
                "V",
                new Interface("CAN", new Lru("Interface Owner LRU")),
                new Lru("Flight Control Unit")
        );

        when(repository.save(signalLibrary)).thenReturn(signalLibrary);

        SignalLibrary result = service.save(signalLibrary);

        assertNotNull(result);
        assertEquals("Flight Control Signals", result.getName());

        verify(repository, times(1)).save(signalLibrary);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnSignalLibraryById() {
        SignalLibrary signalLibrary = new SignalLibrary(
                "Flight Control Signals",
                "Signals for flight control",
                "V",
                new Interface("CAN", new Lru("Interface Owner LRU")),
                new Lru("Flight Control Unit")
        );

        when(repository.findById(1L)).thenReturn(Optional.of(signalLibrary));

        SignalLibrary result = service.getById(1L);

        assertEquals("Flight Control Signals", result.getName());
        assertEquals("CAN", result.getInterfaceEntity().getName());

        verify(repository, times(1)).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowWhenSignalLibraryDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.getById(99L)
        );

        assertEquals("Signal library not found: 99", exception.getMessage());

        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnSignalLibrariesByLruId() {
        SignalLibrary signalLibrary = new SignalLibrary(
                "Power Monitoring Signals",
                "Power distribution monitoring",
                "A",
                new Interface("ARINC 429", new Lru("Interface Owner LRU")),
                new Lru("Power Distribution Unit")
        );

        when(repository.findByLruId(2L)).thenReturn(List.of(signalLibrary));

        List<SignalLibrary> result = service.getByLruId(2L);

        assertEquals(1, result.size());
        assertEquals("Power Monitoring Signals", result.get(0).getName());

        verify(repository, times(1)).findByLruId(2L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnSignalLibrariesByInterfaceId() {
        SignalLibrary signalLibrary = new SignalLibrary(
                "Engine CAN Signals",
                "CAN bus engine telemetry",
                "V",
                new Interface("CAN", new Lru("Interface Owner LRU")),
                new Lru("Engine Control LRU")
        );

        when(repository.findByInterfaceEntityId(1L))
                .thenReturn(List.of(signalLibrary));

        List<SignalLibrary> result = service.getByInterfaceId(1L);

        assertEquals(1, result.size());
        assertEquals("Engine CAN Signals", result.get(0).getName());

        verify(repository, times(1)).findByInterfaceEntityId(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateSignalLibrarySuccessfully() {
        SignalLibrary existing = new SignalLibrary(
                "Old Name",
                "Old description",
                "V",
                new Interface("CAN", new Lru("Interface Owner LRU")),
                new Lru("Flight Control Unit")
        );
        existing.setName("Old Name");

        SignalLibrary incoming = new SignalLibrary(
                "Updated Name",
                "Updated description",
                "A",
                new Interface("ARINC 429", new Lru("Interface Owner LRU")),
                new Lru("Power Distribution Unit")
        );

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        SignalLibrary result = service.update(1L, incoming);

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals("A", result.getEngineeringUnit());

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteSignalLibraryById() {
        service.delete(1L);

        verify(signalRangeRepository, times(1)).deleteBySignalLibraryId(1L);
        verify(repository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(signalRangeRepository);
        verifyNoMoreInteractions(repository);
    }
}

