package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

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

    public Interface getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Interface not found: " + id));
    }

    public Interface update(Long id, Interface iface) {
        Interface existing = getById(id);
        existing.setName(iface.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
