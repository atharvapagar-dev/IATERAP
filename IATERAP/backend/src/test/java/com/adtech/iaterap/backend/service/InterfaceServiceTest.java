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
}
