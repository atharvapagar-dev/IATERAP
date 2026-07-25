package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Aircraft;
import com.adtech.iaterap.backend.repository.AircraftRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AircraftService {

    private final AircraftRepository repository;

    public AircraftService(AircraftRepository repository) {
        this.repository = repository;
    }

    public List<Aircraft> getAllAircraft() {
        return repository.findAll();
    }

    public Aircraft save(Aircraft aircraft) {
        return repository.save(aircraft);
    }
}