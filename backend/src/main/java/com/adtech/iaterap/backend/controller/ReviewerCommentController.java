package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ReviewerCommentRequest;
import com.adtech.iaterap.backend.dto.ReviewerCommentResponse;
import com.adtech.iaterap.backend.service.ReviewerCommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviewer-comments")
public class ReviewerCommentController {

    private final ReviewerCommentService service;

    public ReviewerCommentController(ReviewerCommentService service) {
        this.service = service;
    }

    @PostMapping
    public ReviewerCommentResponse create(@Valid @RequestBody ReviewerCommentRequest request) {
        return service.save(request);
    }

    @GetMapping("/by-revision/{procedureHistoryId}")
    public List<ReviewerCommentResponse> getByRevision(@PathVariable Long procedureHistoryId) {
        return service.getByRevision(procedureHistoryId);
    }
}
