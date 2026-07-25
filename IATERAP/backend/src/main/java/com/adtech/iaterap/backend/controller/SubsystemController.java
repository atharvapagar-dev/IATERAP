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

    @PostMapping
    public Subsystem createSubsystem(
            @RequestBody Subsystem subsystem
    ) {
        return service.save(subsystem);
    }
}