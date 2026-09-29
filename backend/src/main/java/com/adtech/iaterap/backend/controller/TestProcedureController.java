package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.service.TestProcedureService;
import com.adtech.iaterap.backend.service.ProcedureSnapshotService;
import com.adtech.iaterap.backend.entity.ProcedureSnapshot;
import com.adtech.iaterap.backend.entity.CoverageReport;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Minimal backend surface required by the Procedure Builder (PART L) and
 * Version Management (PART O/T). Deliberately small: one create endpoint,
 * one update endpoint (which both add/edit/reorder/remove steps AND, when
 * content actually changed, create a new revision), read endpoints for
 * the procedure itself and its revision history, and (AI-187 / T7-B3) a
 * rollback endpoint nested under the same {@code /history} resource as
 * {@link #getRevision}, since rollback targets one specific historical
 * revision exactly like that read endpoint does.
 */
@RestController
@RequestMapping("/api/procedures")
public class TestProcedureController {

    private final TestProcedureService service;
    private final ProcedureSnapshotService snapshotService;

    public TestProcedureController(TestProcedureService service, ProcedureSnapshotService snapshotService) {
        this.service = service;
        this.snapshotService = snapshotService;
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

    @PostMapping("/{id}/submit-for-review")
    public TestProcedureResponse submitForReview(@PathVariable Long id) {
        return service.submitForReview(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
    public TestProcedureResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
    public TestProcedureResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @GetMapping("/{id}/snapshots/{versionNumber}")
    public ProcedureSnapshot getSnapshot(@PathVariable Long id, @PathVariable Integer versionNumber) {
        return snapshotService.getSnapshot(id, versionNumber);
    }

    @GetMapping("/{id}/coverage-reports/{versionNumber}")
    public CoverageReport getCoverageReport(@PathVariable Long id, @PathVariable Integer versionNumber) {
        return snapshotService.getReport(id, versionNumber);
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
    public List<TestProcedureResponse> getReviewQueue() {
        return service.getReviewQueue();
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

    /**
     * AI-187 / T7-B3: rolls the procedure back to the content of
     * {@code versionNumber}. Non-destructive -- this NEVER edits or
     * removes {@code versionNumber} or any other existing revision; it
     * always appends a brand new revision whose content matches the
     * requested source revision. See
     * {@link TestProcedureService#rollbackToRevision(Long, Integer)}.
     *
     * <p>Returns the live procedure state after rollback, exactly like
     * {@link #update} does for any other content-changing save --
     * {@code response.versionNumber} is the newly created revision
     * number. The full persisted revision is available afterwards via
     * {@link #getRevision}.</p>
     */
    @PostMapping("/{id}/history/{versionNumber}/rollback")
    public TestProcedureResponse rollback(
            @PathVariable Long id,
            @PathVariable Integer versionNumber
    ) {
        return service.rollbackToRevision(id, versionNumber);
    }
}
