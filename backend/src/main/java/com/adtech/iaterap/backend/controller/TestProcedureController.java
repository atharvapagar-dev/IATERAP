package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.service.TestProcedureService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Minimal backend surface required by the Procedure Builder (PART L) and
 * Version Management (PART O/T). Deliberately small: one create endpoint,
 * one update endpoint (which both add/edit/reorder/remove steps AND, when
 * content actually changed, create a new revision), and read endpoints for
 * the procedure itself and its revision history.
 */
@RestController
@RequestMapping("/api/procedures")
public class TestProcedureController {

    private final TestProcedureService service;

    public TestProcedureController(TestProcedureService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestProcedureResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public TestProcedureResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public TestProcedureResponse create(@Valid @RequestBody TestProcedureRequest request) {
        return service.save(request);
    }

    @PutMapping("/{id}")
    public TestProcedureResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TestProcedureRequest request
    ) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/history")
    public List<ProcedureHistoryResponse> getHistory(@PathVariable Long id) {
        return service.getHistory(id);
    }

    @GetMapping("/{id}/history/{versionNumber}")
    public ProcedureHistoryResponse getRevision(
            @PathVariable Long id,
            @PathVariable Integer versionNumber
    ) {
        return service.getRevision(id, versionNumber);
    }
}
