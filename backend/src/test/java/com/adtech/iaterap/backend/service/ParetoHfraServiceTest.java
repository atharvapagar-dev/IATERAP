package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.entity.EventLog;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.EventLogRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ParetoHfraServiceTest {
    @Mock private EventLogRepository events; @Mock private LruRepository lrus;
    @Test void ranksFailuresAndFlagsRepeatOutlierEvidence() {
        Lru dominant = new Lru("Flight Computer"); ReflectionTestUtils.setField(dominant, "id", 1L);
        Lru normal = new Lru("Power Unit"); ReflectionTestUtils.setField(normal, "id", 2L);
        when(lrus.findAll()).thenReturn(List.of(dominant, normal));
        when(events.findAll()).thenReturn(List.of(event(1L), event(1L), event(1L), event(2L), new EventLog(1L, 1L, 1L, "SORTIE_COMPLETED", "INFO", LocalDateTime.now(), "{}", "test")));
        var result = new ParetoHfraService(events, lrus).analyse();
        assertEquals(4L, result.totalFailures()); assertEquals("Flight Computer", result.items().getFirst().lruName());
        assertEquals(75D, result.items().getFirst().contributionPercent()); assertTrue(result.items().getFirst().repeatFailure()); assertTrue(result.items().getFirst().hfraFlag());
    }
    private static EventLog event(Long lruId) { return new EventLog(lruId, lruId, 1L, "FAILURE_DETECTED", "ERROR", LocalDateTime.now(), "{}", "test"); }
}
