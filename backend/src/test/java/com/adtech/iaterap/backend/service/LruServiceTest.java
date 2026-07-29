package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.LruRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LruServiceTest {

    @Mock
    private LruRepository repository;

    private LruService service;

    @BeforeEach
    void setUp() {
        service = new LruService(repository);
    }

    @Test
    void shouldReturnAllLrus() {

        List<Lru> lrus = List.of(
                new Lru("LRU-1"),
                new Lru("LRU-2")
        );

        when(repository.findAll()).thenReturn(lrus);

        List<Lru> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("LRU-1", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoLrusExist() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Lru> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveLruSuccessfully() {

        Lru lru = new Lru("Power Supply");

        when(repository.save(lru)).thenReturn(lru);

        Lru result = service.save(lru);

        assertNotNull(result);
        assertEquals("Power Supply", result.getName());

        verify(repository, times(1)).save(lru);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldGetLruByIdSuccessfully() {
        Lru lru = new Lru("LRU-1");
        when(repository.findById(1L)).thenReturn(Optional.of(lru));

        Lru result = service.getById(1L);

        assertEquals("LRU-1", result.getName());
        verify(repository, times(1)).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowWhenLruNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(99L));

        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateLruSuccessfully() {
        Lru existing = new Lru("Old Name");
        Lru incoming = new Lru("New Name");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Lru result = service.update(1L, incoming);

        assertEquals("New Name", result.getName());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteLruSuccessfully() {
        service.delete(1L);

        verify(repository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(repository);
    }
}
