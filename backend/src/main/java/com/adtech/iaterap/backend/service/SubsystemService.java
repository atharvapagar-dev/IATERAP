package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Subsystem;
import com.adtech.iaterap.backend.repository.SubsystemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SubsystemService {

    private final SubsystemRepository repository;

    public SubsystemService(SubsystemRepository repository) {
        this.repository = repository;
    }

    public List<Subsystem> getAllSubsystems() {
        return repository.findAll();
    }

    public Subsystem save(Subsystem subsystem) {
        return repository.save(subsystem);
    }

    public Subsystem getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Subsystem not found: " + id));
    }

    public Subsystem update(Long id, Subsystem subsystem) {
        Subsystem existing = getById(id);
        existing.setName(subsystem.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
