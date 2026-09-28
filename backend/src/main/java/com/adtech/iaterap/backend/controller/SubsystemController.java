package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Subsystem;
import com.adtech.iaterap.backend.service.SubsystemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subsystems")
public class SubsystemController {

    private final SubsystemService service;

    public SubsystemController(SubsystemService service) {
        this.service = service;
    }

    @GetMapping
    public List<Subsystem> getSubsystems() {
        return service.getAllSubsystems();
    }

    @GetMapping("/{id}")
    public Subsystem getSubsystemById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public Subsystem createSubsystem(
            @RequestBody Subsystem subsystem
    ) {
        return service.save(subsystem);
    }

    @PutMapping("/{id}")
    public Subsystem updateSubsystem(@PathVariable Long id, @RequestBody Subsystem subsystem) {
        return service.update(id, subsystem);
    }

    @DeleteMapping("/{id}")
    public void deleteSubsystem(@PathVariable Long id) {
        service.delete(id);
    }
}
