package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.service.FaultTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fault-templates")
public class FaultTemplateController {

    private final FaultTemplateService service;

    public FaultTemplateController(FaultTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<FaultTemplate> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FaultTemplate getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-lru/{lruId}")
    public List<FaultTemplate> getByLruId(@PathVariable Long lruId) {
        return service.getByLruId(lruId);
    }

    @PostMapping
    public FaultTemplate create(@Valid @RequestBody FaultTemplate faultTemplate) {
        return service.save(faultTemplate);
    }

    @PutMapping("/{id}")
    public FaultTemplate update(
            @PathVariable Long id,
            @Valid @RequestBody FaultTemplate faultTemplate
    ) {
        return service.update(id, faultTemplate);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
