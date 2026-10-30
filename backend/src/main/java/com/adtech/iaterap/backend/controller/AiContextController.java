package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.AiProcedureContextResponse;
import com.adtech.iaterap.backend.dto.ApiResponse;
import com.adtech.iaterap.backend.service.AiContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only isolation boundary for the AI Advisory Service (AI-234). */
@RestController
@RequestMapping("/api/ai/context")
public class AiContextController {
    private final AiContextService service;

    public AiContextController(AiContextService service) {
        this.service = service;
    }

    @GetMapping("/test-procedure/{procedureId}")
    public ApiResponse<AiProcedureContextResponse> getProcedureContext(@PathVariable Long procedureId) {
        return ApiResponse.of(service.getProcedureContext(procedureId));
    }
}
