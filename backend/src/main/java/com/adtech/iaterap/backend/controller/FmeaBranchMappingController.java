package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.FmeaBranchMapping;
import com.adtech.iaterap.backend.service.FmeaBranchMappingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fmea-branch-mappings")
public class FmeaBranchMappingController {

    private final FmeaBranchMappingService service;

    public FmeaBranchMappingController(FmeaBranchMappingService service) {
        this.service = service;
    }

    @GetMapping
    public List<FmeaBranchMapping> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FmeaBranchMapping getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-lru/{lruId}")
    public List<FmeaBranchMapping> getByLruId(@PathVariable Long lruId) {
        return service.getByLruId(lruId);
    }

    @GetMapping("/by-fault-template/{faultTemplateId}")
    public List<FmeaBranchMapping> getByFaultTemplateId(
            @PathVariable Long faultTemplateId
    ) {
        return service.getByFaultTemplateId(faultTemplateId);
    }

    @PostMapping
    public FmeaBranchMapping create(
            @Valid @RequestBody FmeaBranchMapping fmeaBranchMapping
    ) {
        return service.save(fmeaBranchMapping);
    }

    @PutMapping("/{id}")
    public FmeaBranchMapping update(
            @PathVariable Long id,
            @Valid @RequestBody FmeaBranchMapping fmeaBranchMapping
    ) {
        return service.update(id, fmeaBranchMapping);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
