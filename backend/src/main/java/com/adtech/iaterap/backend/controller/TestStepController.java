package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.service.TestStepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/steps")
public class TestStepController {

    private final TestStepService service;

    public TestStepController(TestStepService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public TestStep getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public TestStep update(@PathVariable Long id, @Valid @RequestBody TestStep step) {
        return service.updateStep(id, step);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteStep(id);
    }
}
