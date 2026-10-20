package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.ai.AiAdvisoryClient;
import com.adtech.iaterap.backend.entity.AiSuggestionQueueItem;
import com.adtech.iaterap.backend.entity.AiSuggestionStatus;
import com.adtech.iaterap.backend.repository.AiSuggestionQueueRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiSuggestionQueueServiceTest {
    @Mock private AiAdvisoryClient client;
    @Mock private AiSuggestionQueueRepository repository;
    @Mock private AuditService auditService;
    private AiSuggestionQueueService service;

    @BeforeEach
    void setUp() { service = new AiSuggestionQueueService(client, repository, auditService); }

    @Test
    void acceptsPendingSuggestionAndRecordsEngineerIdentity() {
        AiSuggestionQueueItem item = pendingItem("engineer.a");
        when(repository.findById(11L)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        var result = service.decide(11L, AiSuggestionStatus.ACCEPTED, "engineer.a");

        assertEquals("ACCEPTED", result.status());
        assertEquals("engineer.a", result.decidedBy());
        verify(auditService).record(eq("engineer.a"), eq("ACCEPTED_AI_SUGGESTION"),
                eq("AiSuggestionQueueItem"), eq("11"), eq(null), eq("Draft a controlled recovery check."), any());
    }

    @Test
    void dismissesPendingSuggestionAndRecordsEngineerIdentity() {
        AiSuggestionQueueItem item = pendingItem("engineer.a");
        when(repository.findById(11L)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        var result = service.decide(11L, AiSuggestionStatus.DISMISSED, "engineer.a");

        assertEquals("DISMISSED", result.status());
        assertEquals("engineer.a", result.decidedBy());
        verify(auditService).record(eq("engineer.a"), eq("DISMISSED_AI_SUGGESTION"),
                eq("AiSuggestionQueueItem"), eq("11"), eq(null), eq("Draft a controlled recovery check."), any());
    }

    @Test
    void rejectsDecisionFromDifferentEngineer() {
        when(repository.findById(11L)).thenReturn(Optional.of(pendingItem("engineer.a")));

        assertThrows(IllegalArgumentException.class,
                () -> service.decide(11L, AiSuggestionStatus.ACCEPTED, "engineer.b"));
    }

    @Test
    void rejectsSecondDecisionForTheSameSuggestion() {
        AiSuggestionQueueItem item = pendingItem("engineer.a");
        item.setStatus(AiSuggestionStatus.ACCEPTED);
        when(repository.findById(11L)).thenReturn(Optional.of(item));

        assertThrows(IllegalStateException.class,
                () -> service.decide(11L, AiSuggestionStatus.DISMISSED, "engineer.a"));
    }

    private AiSuggestionQueueItem pendingItem(String requestedBy) {
        AiSuggestionQueueItem item = new AiSuggestionQueueItem();
        ReflectionTestUtils.setField(item, "id", 11L);
        item.setRequestedBy(requestedBy);
        item.setSuggestionText("Draft a controlled recovery check.");
        return item;
    }
}
