package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.LruRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LruService {
    private final LruRepository repository;

    public LruService(LruRepository repository) {
        this.repository = repository;
    }

    public List<Lru> getAll() {
        return repository.findAll();
    }

    public Lru save(Lru lru) {
        return repository.save(lru);
    }

    public Lru getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("LRU not found: " + id));
    }

    public Lru update(Long id, Lru lru) {
        Lru existing = getById(id);
        existing.setName(lru.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
