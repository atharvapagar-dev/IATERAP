package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.LruRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LruService {

    private final LruRepository repository;

    public LruService(LruRepository repository) {
        this.repository = repository;
    }

    public List<Lru> getAll() {
        return repository.findAll();
    }

    public Lru getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LRU not found"));
    }

    public Lru save(Lru lru) {
        return repository.save(lru);
    }

    public Lru update(Long id, Lru updatedLru) {
        Lru existing = getById(id);
        existing.setName(updatedLru.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}