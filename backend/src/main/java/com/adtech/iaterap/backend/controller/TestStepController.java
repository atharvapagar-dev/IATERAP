package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.service.TestStepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Procedure Builder read endpoint.  Keep this under the steps resource so
     * clients do not have to deserialize the full procedure just to refresh
     * its ordered steps.
     */
    @GetMapping("/by-procedure/{procedureId}")
    public List<TestStep> getByProcedure(@PathVariable Long procedureId) {
        return service.getByProcedureId(procedureId);
    }

    @PostMapping("/by-procedure/{procedureId}")
    public TestStep add(@PathVariable Long procedureId, @Valid @RequestBody TestStep step) {
        return service.addStep(procedureId, step);
    }

    @PatchMapping("/by-procedure/{procedureId}/reorder")
    public List<TestStep> reorder(
            @PathVariable Long procedureId,
            @RequestBody List<Long> orderedStepIds
    ) {
        return service.reorderSteps(procedureId, orderedStepIds);
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
