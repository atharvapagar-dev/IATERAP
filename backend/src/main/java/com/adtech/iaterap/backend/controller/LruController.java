package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.service.LruService;
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
    public Lru create(@RequestBody Lru lru) {
        return service.save(lru);
    }
}
