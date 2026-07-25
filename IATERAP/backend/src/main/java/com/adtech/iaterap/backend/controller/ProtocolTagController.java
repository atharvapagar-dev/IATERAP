package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.service.ProtocolTagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/protocol-tags")
public class ProtocolTagController {

    private final ProtocolTagService service;

    public ProtocolTagController(ProtocolTagService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProtocolTag> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProtocolTag getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-lru/{lruId}")
    public List<ProtocolTag> getByLruId(@PathVariable Long lruId) {
        return service.getByLruId(lruId);
    }

    @PostMapping
    public ProtocolTag create(@Valid @RequestBody ProtocolTag protocolTag) {
        return service.save(protocolTag);
    }

    @PutMapping("/{id}")
    public ProtocolTag update(
            @PathVariable Long id,
            @Valid @RequestBody ProtocolTag protocolTag
    ) {
        return service.update(id, protocolTag);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
