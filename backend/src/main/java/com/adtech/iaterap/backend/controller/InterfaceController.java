package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.service.InterfaceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/interfaces")
public class InterfaceController {

    private final InterfaceService service;

    public InterfaceController(InterfaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<Interface> getAll() {
        return service.getAll();
    }

    @PostMapping
    public Interface create(@RequestBody Interface iface) {
        return service.save(iface);
    }
}
