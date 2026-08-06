package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
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
class ConnectorServiceTest {

    private final Lru testLru = new Lru("Test LRU");

    @Mock
    private ConnectorRepository repository;

    private ConnectorService service;

    @BeforeEach
    void setUp() {
        service = new ConnectorService(repository);
    }

    @Test
    void shouldReturnAllConnectors() {
        List<Connector> connectors = List.of(
                new Connector("CONN-A", testLru),
                new Connector("CONN-B", testLru)
        );

        when(repository.findAll()).thenReturn(connectors);

        List<Connector> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("CONN-A", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoConnectorsExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Connector> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveConnectorSuccessfully() {
        Connector connector = new Connector("CONN-C", testLru);

        when(repository.save(connector)).thenReturn(connector);

        Connector result = service.save(connector);

        assertNotNull(result);
        assertEquals("CONN-C", result.getName());

        verify(repository, times(1)).save(connector);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldGetConnectorByIdSuccessfully() {
        Connector connector = new Connector("CONN-A", testLru);
        when(repository.findById(1L)).thenReturn(Optional.of(connector));

        Connector result = service.getById(1L);

        assertEquals("CONN-A", result.getName());
        verify(repository, times(1)).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowWhenConnectorNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(99L));

        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateConnectorSuccessfully() {
        Connector existing = new Connector("Old", testLru);
        Connector incoming = new Connector("New", testLru);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Connector result = service.update(1L, incoming);

        assertEquals("New", result.getName());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteConnectorSuccessfully() {
        service.delete(1L);

        verify(repository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(repository);
    }
}

