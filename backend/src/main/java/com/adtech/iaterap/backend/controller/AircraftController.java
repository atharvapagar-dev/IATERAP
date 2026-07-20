package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Aircraft;
import com.adtech.iaterap.backend.service.AircraftService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final AircraftService service;

    public AircraftController(AircraftService service) {
        this.service = service;
    }

    @GetMapping
    public List<Aircraft> getAircraft() {
        return service.getAllAircraft();
    }

    @PostMapping
    public Aircraft createAircraft(@RequestBody Aircraft aircraft) {
        return service.save(aircraft);
    }
}