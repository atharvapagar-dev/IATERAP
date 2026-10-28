package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.ApiResponse;
import com.adtech.iaterap.backend.service.TestProcedureService;
import com.adtech.iaterap.backend.service.ProcedureSnapshotService;
import com.adtech.iaterap.backend.entity.ProcedureSnapshot;
import com.adtech.iaterap.backend.entity.CoverageReport;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;

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
    public ApiResponse<List<TestProcedureResponse>> getAll(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return page(service.getAll(), cursor, limit);
    }

    @GetMapping("/by-lru/{lruId}")
    public ApiResponse<List<TestProcedureResponse>> getByLru(@PathVariable Long lruId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return page(service.getByLru(lruId), cursor, limit);
    }

    @GetMapping("/{id}")
    public ApiResponse<TestProcedureResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(service.getById(id));
    }

    @PostMapping
    public ApiResponse<TestProcedureResponse> create(@Valid @RequestBody TestProcedureRequest request) {
        return ApiResponse.of(service.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TestProcedureResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TestProcedureRequest request
    ) {
        return ApiResponse.of(service.update(id, request));
    }

    @PostMapping("/{id}/submit-for-review")
    public ApiResponse<TestProcedureResponse> submitForReview(@PathVariable Long id) {
        return ApiResponse.of(service.submitForReview(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
    public ApiResponse<TestProcedureResponse> approve(@PathVariable Long id) {
        return ApiResponse.of(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
    public ApiResponse<TestProcedureResponse> reject(@PathVariable Long id) {
        return ApiResponse.of(service.reject(id));
    }

    @GetMapping("/{id}/snapshots/{versionNumber}")
    public ApiResponse<ProcedureSnapshot> getSnapshot(@PathVariable Long id, @PathVariable Integer versionNumber) {
        return ApiResponse.of(snapshotService.getSnapshot(id, versionNumber));
    }

    @GetMapping("/{id}/coverage-reports/{versionNumber}")
    public ApiResponse<CoverageReport> getCoverageReport(@PathVariable Long id, @PathVariable Integer versionNumber) {
        return ApiResponse.of(snapshotService.getReport(id, versionNumber));
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
    public ApiResponse<List<TestProcedureResponse>> getReviewQueue(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return page(service.getReviewQueue(), cursor, limit);
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<ProcedureHistoryResponse>> getHistory(@PathVariable Long id) {
        return ApiResponse.of(service.getHistory(id));
    }

    @GetMapping("/{id}/history/{versionNumber}")
    public ApiResponse<ProcedureHistoryResponse> getRevision(
            @PathVariable Long id,
            @PathVariable Integer versionNumber
    ) {
        return ApiResponse.of(service.getRevision(id, versionNumber));
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
    public ApiResponse<TestProcedureResponse> rollback(
            @PathVariable Long id,
            @PathVariable Integer versionNumber
    ) {
        return ApiResponse.of(service.rollbackToRevision(id, versionNumber));
    }

    private ApiResponse<List<TestProcedureResponse>> page(
            List<TestProcedureResponse> procedures, Long cursor, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100.");
        List<TestProcedureResponse> eligible = procedures.stream()
                .sorted(Comparator.comparing(TestProcedureResponse::getId))
                .filter(procedure -> cursor == null || procedure.getId() > cursor).toList();
        List<TestProcedureResponse> page = eligible.stream().limit(limit).toList();
        Long nextCursor = eligible.size() > page.size() ? page.getLast().getId() : null;
        return ApiResponse.page(page, page.size(), nextCursor == null ? null : nextCursor.toString());
    }
}
