package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.service.SignalLibraryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signal-libraries")
public class SignalLibraryController {

    private final SignalLibraryService service;

    public SignalLibraryController(SignalLibraryService service) {
        this.service = service;
    }

    @GetMapping
    public List<SignalLibrary> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SignalLibrary getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-lru/{lruId}")
    public List<SignalLibrary> getByLruId(@PathVariable Long lruId) {
        return service.getByLruId(lruId);
    }

    @GetMapping("/by-interface/{interfaceId}")
    public List<SignalLibrary> getByInterfaceId(
            @PathVariable Long interfaceId
    ) {
        return service.getByInterfaceId(interfaceId);
    }

    @PostMapping
    public SignalLibrary create(@Valid @RequestBody SignalLibrary signalLibrary) {
        return service.save(signalLibrary);
    }

    @PutMapping("/{id}")
    public SignalLibrary update(
            @PathVariable Long id,
            @Valid @RequestBody SignalLibrary signalLibrary
    ) {
        return service.update(id, signalLibrary);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
