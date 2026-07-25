package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.entity.SignalRange;
import com.adtech.iaterap.backend.repository.SignalRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalRangeServiceTest {

    @Mock
    private SignalRangeRepository repository;

    private SignalRangeService service;

    @BeforeEach
    void setUp() {
        service = new SignalRangeService(repository);
    }

    @Test
    void shouldReturnAllSignalRanges() {
        SignalRange range = new SignalRange(
                "Normal",
                new BigDecimal("0.00"),
                new BigDecimal("100.00"),
                "NORMAL",
                new SignalLibrary()
        );

        when(repository.findAll()).thenReturn(List.of(range));

        List<SignalRange> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("Normal", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoSignalRangesExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<SignalRange> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveSignalRangeSuccessfully() {
        SignalRange range = new SignalRange(
                "Warning",
                new BigDecimal("100.01"),
                new BigDecimal("120.00"),
                "WARNING",
                new SignalLibrary()
        );

        when(repository.save(range)).thenReturn(range);

        SignalRange result = service.save(range);

        assertNotNull(result);
        assertEquals("Warning", result.getName());

        verify(repository, times(1)).save(range);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateSignalRangeSuccessfully() {
        SignalRange existing = new SignalRange();
        existing.setName("Old");
        existing.setMinimumValue(new BigDecimal("0.00"));
        existing.setMaximumValue(new BigDecimal("50.00"));
        existing.setSeverity("LOW");

        SignalRange incoming = new SignalRange(
                "Updated",
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                "HIGH",
                new SignalLibrary()
        );

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        SignalRange result = service.update(1L, incoming);

        assertEquals("Updated", result.getName());
        assertEquals(new BigDecimal("10.00"), result.getMinimumValue());
        assertEquals(new BigDecimal("80.00"), result.getMaximumValue());
        assertEquals("HIGH", result.getSeverity());

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }
}
