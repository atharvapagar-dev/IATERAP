package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestBlock;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.TestBlockRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Deterministic, non-blocking shared-library reuse advisory; never an AI suggestion. */
@Component
public final class UnusedTestBlockAdvisoryRule implements EngineeringRule {
    private final TestBlockRepository repository;
    public UnusedTestBlockAdvisoryRule(TestBlockRepository repository) { this.repository = repository; }
    @Override public String getRuleId() { return "ADVISORY-UNUSED-TEST-BLOCK"; }
    @Override public RuleCategory getCategory() { return RuleCategory.ADVISORY; }
    @Override public String getDescription() { return "Identify shared Test Blocks not yet reused"; }
    @Override public RuleResult evaluate(TestProcedure procedure, Lru lru) {
        List<String> unused = repository.findAll().stream().filter(block -> block.getUsageCount() != null && block.getUsageCount() == 0)
                .map(TestBlock::getName).toList();
        if (unused.isEmpty()) return RuleResult.pass("All shared Test Blocks have been reused", Map.of("unusedBlockNames", unused));
        return RuleResult.fail("Consider reviewing unused shared Test Blocks for promotion or reuse", Map.of("unusedBlockNames", unused));
    }
}
