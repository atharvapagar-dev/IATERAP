package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestStepSignalRuleTest {

    @Test
    void passesWhenEveryStepReferencesSignalForProcedureLru() {
        Lru lru = new Lru("LRU-1");
        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        TestStep step = new TestStep();
        step.setSignalLibrary(new SignalLibrary("BUS-VOLTAGE", "Voltage", "V", null, lru));

        RuleResult result = new TestStepSignalRule().evaluate(procedure, lru, List.of(step));

        assertTrue(result.isPassed());
    }

    @Test
    void failsWhenStepHasMissingOrCrossLruSignal() {
        Lru procedureLru = new Lru("LRU-1");
        Lru otherLru = new Lru("LRU-2");
        TestProcedure procedure = new TestProcedure();
        procedure.setLru(procedureLru);
        TestStep step = new TestStep();
        step.setSignalLibrary(new SignalLibrary("BUS-VOLTAGE", "Voltage", "V", null, otherLru));

        RuleResult result = new TestStepSignalRule().evaluate(procedure, procedureLru, List.of(step));

        assertFalse(result.isPassed());
        assertTrue(result.getDetails().containsKey("stepIndex"));
    }
}
