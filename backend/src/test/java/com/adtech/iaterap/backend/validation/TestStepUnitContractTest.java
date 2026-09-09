package com.adtech.iaterap.backend.validation;

import com.adtech.iaterap.backend.dto.TestStepResponse;
import com.adtech.iaterap.backend.entity.TestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestStepUnitContractTest {

    @Test
    void exposesExpectedUnitWithoutBreakingQualitativeSteps() {
        TestStep step = new TestStep();
        step.setExpectedBehaviour("Voltage remains within the nominal band");
        step.setExpectedUnit(EngineeringUnit.VOLT);

        TestStepResponse response = TestStepResponse.fromEntity(step);

        assertEquals("VOLT", response.getExpectedUnit());
    }

    @Test
    void leavesExpectedUnitUnsetForExistingQualitativeSteps() {
        TestStepResponse response = TestStepResponse.fromEntity(new TestStep());

        assertNull(response.getExpectedUnit());
    }
}
