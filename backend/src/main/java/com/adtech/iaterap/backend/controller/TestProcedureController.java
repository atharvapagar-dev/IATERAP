package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.service.TestProcedureService;
import com.adtech.iaterap.backend.service.TestStepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Procedure Builder backend + Version Management API.
 */
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
    public List<TestProcedureResponse> getAll() {
        return procedureService.getAll();
    }

    @GetMapping("/{id}")
    public TestProcedureResponse getById(@PathVariable Long id) {
        return procedureService.getById(id);
    }

    @GetMapping("/by-lru/{lruId}")
    public List<TestProcedureResponse> getByLruId(@PathVariable Long lruId) {
        return procedureService.getByLruId(lruId);
    }

    @GetMapping("/by-status/{status}")
    public List<TestProcedureResponse> getByStatus(@PathVariable String status) {
        return procedureService.getByApprovalStatus(status);
    }

    @PostMapping
    public TestProcedureResponse create(
            @Valid @RequestBody TestProcedureRequest request
    ) {
        return procedureService.save(request);
    }

    @PutMapping("/{id}")
    public TestProcedureResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TestProcedureRequest request
    ) {
        return procedureService.update(id, request);
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
    public TestStep addStep(
            @PathVariable Long id,
            @Valid @RequestBody TestStep step
    ) {
        return stepService.addStep(id, step);
    }

    @PatchMapping("/{id}/steps/reorder")
    public List<TestStep> reorderSteps(
            @PathVariable Long id,
            @RequestBody List<Long> orderedStepIds
    ) {
        return stepService.reorderSteps(id, orderedStepIds);
    }

    @GetMapping("/{id}/history")
    public List<ProcedureHistoryResponse> getHistory(
            @PathVariable Long id
    ) {
        return procedureService.getHistory(id);
    }

    @GetMapping("/{id}/history/{versionNumber}")
    public ProcedureHistoryResponse getRevision(
            @PathVariable Long id,
            @PathVariable Integer versionNumber
    ) {
        return procedureService.getRevision(id, versionNumber);
    }
}