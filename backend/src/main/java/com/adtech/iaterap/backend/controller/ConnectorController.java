package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.service.ConnectorService;
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
    public Connector create(@RequestBody Connector connector) {
        return service.save(connector);
    }
}
