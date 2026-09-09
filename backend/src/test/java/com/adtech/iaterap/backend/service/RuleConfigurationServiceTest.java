package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import com.adtech.iaterap.backend.dto.CoverageScoringConfigurationRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleConfigurationServiceTest {
    private final RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
    private final RuleConfigurationService service = new RuleConfigurationService(repository);

    @Test
    void enableAndDisableArePersisted() {
        RuleConfiguration rule = rule();
        when(repository.findByRuleId("STRUCT-1")).thenReturn(Optional.of(rule));
        when(repository.save(rule)).thenReturn(rule);

        service.setEnabled("STRUCT-1", false);
        assertFalse(rule.isEnabled());
        service.setEnabled("STRUCT-1", true);
        assertTrue(rule.isEnabled());
        verify(repository, times(2)).save(rule);
    }

    @Test
    void priorityChangeAndInvalidValuesAreHandled() {
        RuleConfiguration rule = rule();
        when(repository.findByRuleId("STRUCT-1")).thenReturn(Optional.of(rule));
        when(repository.save(rule)).thenReturn(rule);

        service.setPriority("STRUCT-1", 50);
        assertEquals(50, rule.getPriority());
        assertThrows(IllegalArgumentException.class, () -> service.setPriority("STRUCT-1", -1));
        assertThrows(java.util.NoSuchElementException.class, () -> service.setEnabled("UNKNOWN", true));
    }

    @Test
    void disablingCoverageRuleThatBreaksWeightTotalIsRejected() {
        RuleConfiguration fault = coverage("COVERAGE-FAULT", "25", true);
        when(repository.findByRuleId("COVERAGE-FAULT")).thenReturn(Optional.of(fault));
        when(repository.findAll()).thenReturn(defaultCoverageRules(coverage("COVERAGE-FAULT", "25", true), true));

        assertThrows(IllegalStateException.class, () -> service.setEnabled("COVERAGE-FAULT", false));
        assertTrue(fault.isEnabled());
        verify(repository, never()).save(any());
    }

    @Test
    void validZeroWeightCoverageEnableAndDisableAreAccepted() {
        RuleConfiguration fault = coverage("COVERAGE-FAULT", "0", true);
        when(repository.findByRuleId("COVERAGE-FAULT")).thenReturn(Optional.of(fault));
        when(repository.findAll()).thenReturn(balancedCoverageRules(coverage("COVERAGE-FAULT", "0", true), true));
        when(repository.save(fault)).thenReturn(fault);

        service.setEnabled("COVERAGE-FAULT", false);
        assertFalse(fault.isEnabled());
        service.setEnabled("COVERAGE-FAULT", true);
        assertTrue(fault.isEnabled());
        verify(repository, times(2)).save(fault);
    }

    @Test
    void invalidCoverageWeightConfigurationIsRejectedBeforePersistence() {
        CoverageScoringConfigurationRequest request = new CoverageScoringConfigurationRequest(List.of(
                item("COVERAGE-FAULT", "25"), item("COVERAGE-FMEA", "20"), item("COVERAGE-REQUIREMENT", "20"),
                item("COVERAGE-INTERFACE", "15"), item("COVERAGE-SIGNAL", "10"), item("COVERAGE-FUNCTIONAL", "9")
        ));
        assertThrows(IllegalArgumentException.class, () -> service.updateCoverageScoringConfiguration(request));
        verify(repository, never()).saveAll(any());
    }

    private CoverageScoringConfigurationRequest.Item item(String ruleId, String weight) {
        return new CoverageScoringConfigurationRequest.Item(ruleId, new BigDecimal(weight), BigDecimal.ZERO, true);
    }

    private List<RuleConfiguration> defaultCoverageRules(RuleConfiguration fault, boolean faultEnabled) {
        fault.setEnabled(faultEnabled);
        return List.of(fault, coverage("COVERAGE-FMEA", "20", true), coverage("COVERAGE-REQUIREMENT", "20", true),
                coverage("COVERAGE-INTERFACE", "15", true), coverage("COVERAGE-SIGNAL", "10", true), coverage("COVERAGE-FUNCTIONAL", "10", true));
    }

    private List<RuleConfiguration> balancedCoverageRules(RuleConfiguration fault, boolean faultEnabled) {
        fault.setEnabled(faultEnabled);
        return List.of(fault, coverage("COVERAGE-FMEA", "20", true), coverage("COVERAGE-REQUIREMENT", "20", true),
                coverage("COVERAGE-INTERFACE", "20", true), coverage("COVERAGE-SIGNAL", "20", true), coverage("COVERAGE-FUNCTIONAL", "20", true));
    }

    private RuleConfiguration coverage(String id, String weight, boolean enabled) {
        RuleConfiguration rule = new RuleConfiguration(id, RuleCategory.COVERAGE, 10, id, enabled);
        rule.setWeight(new BigDecimal(weight));
        rule.setThreshold(BigDecimal.ZERO);
        return rule;
    }

    private RuleConfiguration rule() {
        return new RuleConfiguration("STRUCT-1", RuleCategory.STRUCTURAL, 10, "Structural rule", true);
    }
}
