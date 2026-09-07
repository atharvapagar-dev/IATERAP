package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Structural rule ensuring every step references a signal from the procedure LRU. */
@Component
public final class TestStepSignalRule implements EngineeringRule {

    @Override public String getRuleId() { return "STRUCT-TEST-STEP-SIGNAL"; }
    @Override public RuleCategory getCategory() { return RuleCategory.STRUCTURAL; }
    @Override public int getPriority() { return 10; }
    @Override public String getDescription() { return "Every Test Step must reference a valid Signal for the procedure LRU"; }

    @Override
    public RuleResult evaluate(TestProcedure procedure, Lru lru) {
        return evaluate(procedure, lru, List.of());
    }

    @Override
    public RuleResult evaluate(TestProcedure procedure, Lru lru, List<TestStep> steps) {
        if (procedure == null || lru == null) {
            return RuleResult.fail("Procedure and LRU context are required");
        }
        if (procedure.getLru() == null || !sameId(procedure.getLru(), lru)) {
            return RuleResult.fail("Procedure LRU does not match the supplied LRU context");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        for (int index = 0; index < steps.size(); index++) {
            TestStep step = steps.get(index);
            if (step == null || step.getSignalLibrary() == null
                    || step.getSignalLibrary().getLru() == null
                    || !sameId(step.getSignalLibrary().getLru(), lru)) {
                details.put("stepIndex", index);
                details.put("reason", "missing or cross-LRU signal reference");
                return RuleResult.fail("Every Test Step must reference a valid Signal for the procedure LRU", details);
            }
        }
        details.put("validatedStepCount", steps.size());
        return RuleResult.pass("All Test Steps reference valid Signals for the procedure LRU", details);
    }

    private boolean sameId(Lru left, Lru right) {
        return left == right || (left.getId() != null && left.getId().equals(right.getId()));
    }
}
