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

    public Lru save(Lru lru) {
        return repository.save(lru);
    }
}
