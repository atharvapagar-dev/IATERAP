package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProtocolTagService {

    private final ProtocolTagRepository repository;

    public ProtocolTagService(ProtocolTagRepository repository) {
        this.repository = repository;
    }

    public List<ProtocolTag> getAll() {
        return repository.findAll();
    }

    public List<ProtocolTag> getByLruId(Long lruId) {
        return repository.findByLruId(lruId);
    }

    public ProtocolTag getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Protocol tag not found: " + id
                ));
    }

    public ProtocolTag save(ProtocolTag protocolTag) {
        return repository.save(protocolTag);
    }

    public ProtocolTag update(Long id, ProtocolTag protocolTag) {
        ProtocolTag existing = getById(id);

        existing.setName(protocolTag.getName());
        existing.setValue(protocolTag.getValue());
        existing.setDescription(protocolTag.getDescription());
        existing.setLru(protocolTag.getLru());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
