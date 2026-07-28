package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.service.InterfaceService;
import jakarta.validation.Valid;
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
    public Interface create(@Valid @RequestBody Interface iface) {
        return service.save(iface);
    }

    @GetMapping("/{id}")
    public Interface getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Interface update(@PathVariable Long id, @Valid @RequestBody Interface iface) {

    public Interface update(@PathVariable Long id, @RequestBody Interface iface) {
        return service.update(id, iface);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
