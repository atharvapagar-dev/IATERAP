package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** AI-248 storage contract for simulated fleet instances and immutable events. */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class SyntheticEventStorageContractTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Long instanceId;

    @AfterEach
    void cleanUp() {
        if (instanceId != null) {
            jdbc.update("DELETE FROM event_log WHERE synthetic_lru_instance_id = ?", instanceId);
            jdbc.update("DELETE FROM synthetic_lru_instance WHERE id = ?", instanceId);
        }
    }

    @Test
    void migrationCreatesSyntheticInstanceAndEventTablesWithRequiredIndexes() {
        assertTrue(tableExists("synthetic_lru_instance"));
        assertTrue(tableExists("event_log"));
        assertEquals("lru", foreignKeyTarget("synthetic_lru_instance", "lru_id"));
        assertTrue(indexDefinitions("event_log").stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("lru_id")
                        && value.contains("event_type") && value.contains("timestamp")));
    }

    @Test
    void eventCannotClaimAnLruDifferentFromItsSyntheticInstance() {
        List<Long> lruIds = jdbc.queryForList("SELECT id FROM lru ORDER BY id LIMIT 2", Long.class);
        assertTrue(lruIds.size() >= 2, "Synthetic test profile must provide two LRUs");
        Long sourceLruId = lruIds.get(0);
        Long otherLruId = lruIds.get(1);
        instanceId = jdbc.queryForObject(
                "INSERT INTO synthetic_lru_instance (lru_id, instance_code, generator_seed) VALUES (?, ?, ?) RETURNING id",
                Long.class, sourceLruId, "AI248-CONTRACT-INSTANCE", 248L);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO event_log (synthetic_lru_instance_id, lru_id, event_type, timestamp) VALUES (?, ?, ?, ?)",
                instanceId, otherLruId, "SYNTHETIC_FAULT", Timestamp.valueOf(LocalDateTime.now())));

        jdbc.update("INSERT INTO event_log (synthetic_lru_instance_id, lru_id, event_type, timestamp) VALUES (?, ?, ?, ?)",
                instanceId, sourceLruId, "SYNTHETIC_FAULT", Timestamp.valueOf(LocalDateTime.now()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_log WHERE synthetic_lru_instance_id = ?", Integer.class, instanceId));
    }

    private boolean tableExists(String tableName) {
        Integer matches = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class, tableName);
        return matches != null && matches == 1;
    }

    private String foreignKeyTarget(String tableName, String columnName) {
        return jdbc.queryForObject(
                "SELECT ccu.table_name FROM information_schema.table_constraints tc "
                        + "JOIN information_schema.key_column_usage kcu "
                        + "ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema "
                        + "JOIN information_schema.constraint_column_usage ccu "
                        + "ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema "
                        + "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = current_schema() "
                        + "AND tc.table_name = ? AND kcu.column_name = ?",
                String.class, tableName, columnName);
    }

    private List<String> indexDefinitions(String tableName) {
        return jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = current_schema() AND tablename = ?",
                String.class, tableName);
    }
}
