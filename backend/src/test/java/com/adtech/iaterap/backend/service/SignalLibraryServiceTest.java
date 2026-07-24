package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalLibraryServiceTest {

    @Mock
    private SignalLibraryRepository repository;

    private SignalLibraryService service;

    @BeforeEach
    void setUp() {
        service = new SignalLibraryService(repository);
    }

    @Test
    void shouldReturnAllSignalLibraries() {
        SignalLibrary first = new SignalLibrary(
                "Engine Signals",
                "Core engine telemetry",
                "V",
                new Interface("CAN"),
                new Lru("Flight Control Unit")
        );
        SignalLibrary second = new SignalLibrary(
                "Cabin Signals",
                "Cabin monitoring telemetry",
                "A",
                new Interface("ARINC 429"),
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
                new Interface("CAN"),
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
    void shouldUpdateSignalLibrarySuccessfully() {
        SignalLibrary existing = new SignalLibrary(
                "Old Name",
                "Old description",
                "V",
                new Interface("CAN"),
                new Lru("Flight Control Unit")
        );
        existing.setName("Old Name");

        SignalLibrary incoming = new SignalLibrary(
                "Updated Name",
                "Updated description",
                "A",
                new Interface("ARINC 429"),
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
}
