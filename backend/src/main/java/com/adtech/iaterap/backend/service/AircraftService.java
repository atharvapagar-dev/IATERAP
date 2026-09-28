package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Aircraft;
import com.adtech.iaterap.backend.repository.AircraftRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

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

    public Aircraft getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aircraft not found: " + id));
    }

    public Aircraft update(Long id, Aircraft aircraft) {
        Aircraft existing = getById(id);
        existing.setName(aircraft.getName());
        existing.setManufacturer(aircraft.getManufacturer());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
