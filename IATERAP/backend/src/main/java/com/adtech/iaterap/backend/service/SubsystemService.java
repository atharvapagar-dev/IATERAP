package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Subsystem;
import com.adtech.iaterap.backend.repository.SubsystemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
}