package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.FmeaBranchMapping;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import org.springframework.stereotype.Component;

/**
 * A Test Step covers each defined FMEA branch mapped to its referenced FaultTemplate.
 */
@Component
public final class FmeaCoverageRule extends AbstractCoverageCategoryRule {
    private final FmeaBranchMappingRepository repository;
    public FmeaCoverageRule(FmeaBranchMappingRepository repository) {
        super("COVERAGE-FMEA", "FMEA branches derived from referenced fault templates");
        this.repository = repository;
    }
    @Override public RuleResult evaluate(TestProcedure procedure, Lru lru, java.util.List<TestStep> steps) {
        if (lru == null || lru.getId() == null) return unavailable("An LRU context is required");
        java.util.List<FmeaBranchMapping> mappings = repository.findByLruId(lru.getId());
        java.util.Set<Long> defined = mappings.stream().map(FmeaBranchMapping::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> faults = (steps == null ? java.util.List.<TestStep>of() : steps).stream().map(TestStep::getFaultCategory).filter(java.util.Objects::nonNull).map(f -> f.getId()).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> referenced = mappings.stream().filter(m -> m.getFaultTemplate() != null && faults.contains(m.getFaultTemplate().getId())).map(FmeaBranchMapping::getId).collect(java.util.stream.Collectors.toSet());
        return measured(defined, referenced);
    }
}
