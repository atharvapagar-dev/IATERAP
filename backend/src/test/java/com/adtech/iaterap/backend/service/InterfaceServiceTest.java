package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
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
class InterfaceServiceTest {

    @Mock
    private InterfaceRepository repository;

    private InterfaceService service;

    @BeforeEach
    void setUp() {
        service = new InterfaceService(repository);
    }

    @Test
    void shouldReturnAllInterfaces() {

        List<Interface> interfaces = List.of(
                new Interface("CAN"),
                new Interface("ARINC")
        );

        when(repository.findAll()).thenReturn(interfaces);

        List<Interface> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("CAN", result.get(0).getName());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoInterfacesExist() {

        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Interface> result = service.getAll();

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldSaveInterfaceSuccessfully() {

        Interface iface = new Interface("Ethernet");

        when(repository.save(iface)).thenReturn(iface);

        Interface result = service.save(iface);

        assertNotNull(result);
        assertEquals("Ethernet", result.getName());

        verify(repository, times(1)).save(iface);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldGetInterfaceByIdSuccessfully() {
        Interface iface = new Interface("CAN");
        when(repository.findById(1L)).thenReturn(Optional.of(iface));

        Interface result = service.getById(1L);

        assertEquals("CAN", result.getName());
        verify(repository, times(1)).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowWhenInterfaceNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(99L));

        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateInterfaceSuccessfully() {
        Interface existing = new Interface("Old");
        Interface incoming = new Interface("New");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Interface result = service.update(1L, incoming);

        assertEquals("New", result.getName());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteInterfaceSuccessfully() {
        service.delete(1L);

        verify(repository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(repository);
    }
}
