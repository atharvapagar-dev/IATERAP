package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.service.TestProcedureService;
import com.adtech.iaterap.backend.service.TestStepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedures")
public class TestProcedureController {

    private final TestProcedureService procedureService;
    private final TestStepService stepService;

    public TestProcedureController(
            TestProcedureService procedureService,
            TestStepService stepService
    ) {
        this.procedureService = procedureService;
        this.stepService = stepService;
    }

    @GetMapping
    public List<TestProcedure> getAll() {
        return procedureService.getAll();
    }

    @GetMapping("/{id}")
    public TestProcedure getById(@PathVariable Long id) {
        return procedureService.getById(id);
    }

    @GetMapping("/by-lru/{lruId}")
    public List<TestProcedure> getByLruId(@PathVariable Long lruId) {
        return procedureService.getByLruId(lruId);
    }

    @GetMapping("/by-status/{status}")
    public List<TestProcedure> getByStatus(@PathVariable String status) {
        return procedureService.getByApprovalStatus(status);
    }

    @PostMapping
    public TestProcedure create(@Valid @RequestBody TestProcedure procedure) {
        return procedureService.create(procedure);
    }

    @PutMapping("/{id}")
    public TestProcedure update(
            @PathVariable Long id,
            @Valid @RequestBody TestProcedure procedure
    ) {
        return procedureService.update(id, procedure);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        procedureService.delete(id);
    }

    @GetMapping("/{id}/steps")
    public List<TestStep> getSteps(@PathVariable Long id) {
        return stepService.getByProcedureId(id);
    }

    @PostMapping("/{id}/steps")
    public TestStep addStep(@PathVariable Long id, @Valid @RequestBody TestStep step) {
        return stepService.addStep(id, step);
    }

    @PatchMapping("/{id}/steps/reorder")
    public List<TestStep> reorderSteps(
            @PathVariable Long id,
            @RequestBody List<Long> orderedStepIds
    ) {
        return stepService.reorderSteps(id, orderedStepIds);
    }
}
