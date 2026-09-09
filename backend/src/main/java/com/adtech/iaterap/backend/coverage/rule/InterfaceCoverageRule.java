package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import org.springframework.stereotype.Component;

/** Coverage of steps linked to an Interface. */
@Component
public final class InterfaceCoverageRule extends AbstractCoverageCategoryRule {
    private final InterfaceRepository repository;

    public InterfaceCoverageRule(InterfaceRepository repository) {
        super("COVERAGE-INTERFACE", "Interface elements referenced by test steps");
        this.repository = repository;
    }

    @Override
    public RuleResult evaluate(TestProcedure procedure, com.adtech.iaterap.backend.entity.Lru lru, java.util.List<TestStep> steps) {
        if (lru == null || lru.getId() == null) return unavailable("An LRU context is required");
        java.util.Set<Long> defined = repository.findByLruId(lru.getId()).stream()
                .map(com.adtech.iaterap.backend.entity.Interface::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> referenced = (steps == null ? java.util.List.<TestStep>of() : steps).stream()
                .map(TestStep::getInterfaceEntity).filter(java.util.Objects::nonNull)
                .map(com.adtech.iaterap.backend.entity.Interface::getId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return measured(defined, referenced);
    }
}
