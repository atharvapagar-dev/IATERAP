package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.RuleConfigurationResponse;
import com.adtech.iaterap.backend.repository.UserRepository;
import com.adtech.iaterap.backend.security.JwtService;
import com.adtech.iaterap.backend.service.RuleConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRuleConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminRuleConfigurationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RuleConfigurationService service;
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    @Test
    void rulesUseTheStandardEnvelopeAndCursorMetadata() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new RuleConfigurationResponse("RULE-A", "STRUCTURAL", 1, "A", true, null, null),
                new RuleConfigurationResponse("RULE-B", "STRUCTURAL", 2, "B", true, null, null)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/rules")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ruleId").value("RULE-A"))
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.meta.nextCursor").value("RULE-A"));
    }

    @Test
    void invalidPriorityUsesTheStandardValidationErrorResponse() throws Exception {
        mockMvc.perform(patch("/api/admin/rules/STRUCT-TEST-STEP-SIGNAL/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields."))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.priority").exists());
    }

    @Test
    void unknownRuleIdentifierUsesTheStandardNotFoundErrorResponse() throws Exception {
        doThrow(new NoSuchElementException("Rule configuration not found: UNKNOWN"))
                .when(service).setEnabled("UNKNOWN", true);

        mockMvc.perform(patch("/api/admin/rules/UNKNOWN/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rule configuration not found: UNKNOWN"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verify(service).setEnabled("UNKNOWN", true);
    }
}
