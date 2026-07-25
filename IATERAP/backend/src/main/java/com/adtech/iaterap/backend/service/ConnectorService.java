package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import org.springframework.stereotype.Service;
import java.util.List;

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
}
