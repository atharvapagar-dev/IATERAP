package com.adtech.iaterap.backend.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScoringConfigurationService;
import com.adtech.iaterap.backend.dto.AiSuggestionRequest;
import com.adtech.iaterap.backend.service.ProcedureSnapshotService;
import com.adtech.iaterap.backend.service.TestProcedureService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** AI-260: core engineering work must remain usable when FastAPI is unavailable. */
class AiServiceIsolationVerificationTest {
    @Test
    void unavailableAiServiceDoesNotAffectCoverageEngine() {
        AiAdvisoryClient unavailable = new AiAdvisoryClient("http://127.0.0.1:1", 50, 50);
        assertThrows(AiAdvisoryUnavailableException.class,
                () -> unavailable.suggest(new AiSuggestionRequest(1L, 1L, "unavailable by design")));

        CoverageScoringConfigurationService configuration = mock(CoverageScoringConfigurationService.class);
        when(configuration.activeCoverageRules()).thenReturn(List.of());
        CoverageEngine coverage = new CoverageEngine(null, configuration);
        assertDoesNotThrow(() -> coverage.calculate(Map.of()));
    }

    @Test
    void authoringCoverageAndApprovalCoreHaveNoAiServiceDependency() {
        assertNoAiDependency(TestProcedureService.class);
        assertNoAiDependency(CoverageEngine.class);
        assertNoAiDependency(ProcedureSnapshotService.class);
    }

    private static void assertNoAiDependency(Class<?> type) {
        for (Field field : type.getDeclaredFields()) noAiType(field.getType());
        for (Constructor<?> constructor : type.getDeclaredConstructors())
            for (Class<?> parameter : constructor.getParameterTypes()) noAiType(parameter);
    }

    private static void noAiType(Class<?> type) {
        if (type.getPackageName().contains(".ai") || type.getSimpleName().startsWith("Ai")) {
            throw new AssertionError("Core component must not depend on AI service: " + type.getName());
        }
    }
}
