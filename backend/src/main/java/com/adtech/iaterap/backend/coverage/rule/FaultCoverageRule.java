package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.entity.FaultTemplate;

/** Coverage of steps linked to a Fault Template. */
@Component
public final class FaultCoverageRule extends AbstractCoverageCategoryRule {
    private final FaultTemplateRepository repository;
    public FaultCoverageRule(FaultTemplateRepository repository) {
        super("COVERAGE-FAULT", "Fault elements referenced by test steps");
        this.repository = repository;
    }
    @Override public RuleResult evaluate(TestProcedure procedure, Lru lru, java.util.List<TestStep> steps) {
        if (lru == null || lru.getId() == null) return unavailable("An LRU context is required");
        java.util.Set<Long> defined = repository.findByLruId(lru.getId()).stream().map(FaultTemplate::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> referenced = (steps == null ? java.util.List.<TestStep>of() : steps).stream().map(TestStep::getFaultCategory).filter(java.util.Objects::nonNull).map(FaultTemplate::getId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        return measured(defined, referenced);
    }
}
