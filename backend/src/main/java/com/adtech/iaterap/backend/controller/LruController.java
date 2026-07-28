package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.service.LruService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lrus")
public class LruController {

    private final LruService service;

    public LruController(LruService service) {
        this.service = service;
    }

    @GetMapping
    public List<Lru> getAll() {
        return service.getAll();
    }

    @PostMapping
    public Lru create(@Valid @RequestBody Lru lru) {
        return service.save(lru);
    }

    @GetMapping("/{id}")
    public Lru getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Lru update(@PathVariable Long id, @Valid @RequestBody Lru lru) {
        return service.update(id, lru);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
