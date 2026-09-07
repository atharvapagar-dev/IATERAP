package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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

    private RuleConfiguration rule() {
        return new RuleConfiguration("STRUCT-1", RuleCategory.STRUCTURAL, 10, "Structural rule", true);
    }
}