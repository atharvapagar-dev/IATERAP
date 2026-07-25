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
}
