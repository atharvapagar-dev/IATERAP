package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.service.ConnectorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {

    private final ConnectorService service;

    public ConnectorController(ConnectorService service) {
        this.service = service;
    }

    @GetMapping
    public List<Connector> getAll() {
        return service.getAll();
    }

    @PostMapping
    public Connector create(@Valid @RequestBody Connector connector) {
        return service.save(connector);
    }

    @GetMapping("/{id}")
    public Connector getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Connector update(@PathVariable Long id, @Valid @RequestBody Connector connector) {
        return service.update(id, connector);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
