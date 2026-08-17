package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.*;
import com.adtech.iaterap.backend.service.TestBlockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** AI-179 [T7-A1]: organisation-wide reusable Test Block library. */
@RestController
@RequestMapping("/api/test-blocks")
public class TestBlockController {

    private final TestBlockService service;

    public TestBlockController(TestBlockService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestBlockResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public TestBlockResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("/from-step/{testStepId}")
    public TestBlockResponse createFromStep(
            @PathVariable Long testStepId,
            @Valid @RequestBody CreateTestBlockFromStepRequest request
    ) {
        return service.createFromStep(testStepId, request);
    }

    @PostMapping("/{id}/insert")
    public TestProcedureResponse insertIntoProcedure(
            @PathVariable Long id,
            @Valid @RequestBody InsertTestBlockRequest request
    ) {
        return service.insertIntoProcedure(id, request);
    }
}