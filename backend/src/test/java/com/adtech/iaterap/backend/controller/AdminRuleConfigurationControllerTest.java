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
    void invalidPriorityUsesTheStandardValidationErrorResponse() throws Exception {
        mockMvc.perform(patch("/api/admin/rules/STRUCT-TEST-STEP-SIGNAL/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields."))
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
                .andExpect(jsonPath("$.message").value("Rule configuration not found: UNKNOWN"));

        verify(service).setEnabled("UNKNOWN", true);
    }
}
