package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.TestBlock;
import com.adtech.iaterap.backend.repository.TestBlockRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnusedTestBlockAdvisoryRuleTest {
    @Test void reportsOnlyZeroUsageBlocksAsDeterministicNonBlockingAdvisory() {
        TestBlockRepository repository = mock(TestBlockRepository.class);
        TestBlock unused = new TestBlock("Reusable setup", "description"); unused.setUsageCount(0);
        TestBlock used = new TestBlock("Used setup", "description"); used.setUsageCount(2);
        when(repository.findAll()).thenReturn(List.of(unused, used));

        RuleResult result = new UnusedTestBlockAdvisoryRule(repository).evaluate(null, null);

        assertFalse(result.isPassed());
        assertEquals(List.of("Reusable setup"), result.getDetails().get("unusedBlockNames"));
    }
}
