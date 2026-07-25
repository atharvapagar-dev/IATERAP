package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InterfaceService {
    private final InterfaceRepository repository;

    public InterfaceService(InterfaceRepository repository) {
        this.repository = repository;
    }

    public List<Interface> getAll() {
        return repository.findAll();
    }

    public Interface save(Interface iface) {
        return repository.save(iface);
    }
}
