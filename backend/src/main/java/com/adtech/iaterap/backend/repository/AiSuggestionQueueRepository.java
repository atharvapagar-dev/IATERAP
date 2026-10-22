package com.adtech.iaterap.backend.repository;
import com.adtech.iaterap.backend.entity.AiSuggestionQueueItem;
import com.adtech.iaterap.backend.entity.AiSuggestionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AiSuggestionQueueRepository extends JpaRepository<AiSuggestionQueueItem, Long> {
    List<AiSuggestionQueueItem> findByRequestedByAndStatusOrderByCreatedAtDesc(String requestedBy, AiSuggestionStatus status);
    List<AiSuggestionQueueItem> findByRequestedByAndTestProcedureIdAndStatusOrderByDecidedAtDesc(
            String requestedBy, Long testProcedureId, AiSuggestionStatus status);
}
