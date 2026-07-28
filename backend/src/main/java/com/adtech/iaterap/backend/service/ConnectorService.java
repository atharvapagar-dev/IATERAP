package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ConnectorService {
    private final ConnectorRepository repository;

    public ConnectorService(ConnectorRepository repository) {
        this.repository = repository;
    }

    public List<Connector> getAll() {
        return repository.findAll();
    }

    public Connector save(Connector connector) {
        return repository.save(connector);
    }

    public Connector getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Connector not found: " + id));
    }

    public Connector update(Long id, Connector connector) {
        Connector existing = getById(id);
        existing.setName(connector.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
