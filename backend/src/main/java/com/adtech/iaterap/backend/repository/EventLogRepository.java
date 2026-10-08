package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.EventLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByLruIdAndEventTypeAndTimestampBetweenOrderByTimestampDesc(
            Long lruId, String eventType, LocalDateTime from, LocalDateTime to);
}
