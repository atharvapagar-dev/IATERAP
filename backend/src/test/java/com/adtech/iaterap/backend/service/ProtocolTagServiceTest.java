package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
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
class ProtocolTagServiceTest {

    @Mock
    private ProtocolTagRepository repository;

    private ProtocolTagService service;

    @BeforeEach
    void setUp() {
        service = new ProtocolTagService(repository);
    }

    @Test
    void shouldReturnAllProtocolTags() {
        ProtocolTag tag = new ProtocolTag(
                "CAN Mode",
                "CAN2.0B",
                "Primary protocol tag",
                new Lru("Flight Control Unit")
        );

        when(repository.findAll()).thenReturn(List.of(tag));

        List<ProtocolTag> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("CAN Mode", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoProtocolTagsExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<ProtocolTag> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveProtocolTagSuccessfully() {
        ProtocolTag tag = new ProtocolTag(
                "ARINC Label",
                "204",
                "Primary ARINC label",
                new Lru("Power Distribution Unit")
        );

        when(repository.save(tag)).thenReturn(tag);

        ProtocolTag result = service.save(tag);

        assertNotNull(result);
        assertEquals("ARINC Label", result.getName());

        verify(repository, times(1)).save(tag);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateProtocolTagSuccessfully() {
        ProtocolTag existing = new ProtocolTag(
                "Old",
                "100",
                "Old description",
                new Lru("Flight Control Unit")
        );

        ProtocolTag incoming = new ProtocolTag(
                "Updated",
                "205",
                "Updated description",
                new Lru("Power Distribution Unit")
        );

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        ProtocolTag result = service.update(1L, incoming);

        assertEquals("Updated", result.getName());
        assertEquals("205", result.getValue());
        assertEquals("Updated description", result.getDescription());

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }
}
