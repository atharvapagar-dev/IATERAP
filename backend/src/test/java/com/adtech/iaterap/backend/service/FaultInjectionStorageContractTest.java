package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-239 / T13-SH: verifies that fault-injection evidence remains anchored to
 * an immutable procedure snapshot instead of mutable live procedure tables.
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class FaultInjectionStorageContractTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationCreatesRunAndReportTablesWithSnapshotForeignKeys() {
        assertTrue(tableExists("fault_injection_run"));
        assertTrue(tableExists("validation_report"));

        assertEquals("procedure_snapshot", foreignKeyTarget("fault_injection_run", "procedure_snapshot_id"));
        assertEquals("fault_injection_run", foreignKeyTarget("validation_report", "fault_injection_run_id"));
        assertEquals("procedure_snapshot", foreignKeyTarget("validation_report", "procedure_snapshot_id"));
    }

    @Test
    void migrationCreatesRevisionScopedRunAndReportIndexes() {
        assertTrue(indexDefinitions("fault_injection_run").stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("procedure_snapshot_id") && value.contains("created_at")));
        assertTrue(indexDefinitions("validation_report").stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("procedure_snapshot_id") && value.contains("generated_at")));
    }

    private boolean tableExists(String tableName) {
        Integer matches = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class,
                tableName);
        return matches != null && matches == 1;
    }

    private String foreignKeyTarget(String tableName, String columnName) {
        return jdbc.queryForObject(
                "SELECT ccu.table_name "
                        + "FROM information_schema.table_constraints tc "
                        + "JOIN information_schema.key_column_usage kcu "
                        + "  ON tc.constraint_name = kcu.constraint_name "
                        + " AND tc.table_schema = kcu.table_schema "
                        + "JOIN information_schema.constraint_column_usage ccu "
                        + "  ON ccu.constraint_name = tc.constraint_name "
                        + " AND ccu.table_schema = tc.table_schema "
                        + "WHERE tc.constraint_type = 'FOREIGN KEY' "
                        + "  AND tc.table_schema = current_schema() "
                        + "  AND tc.table_name = ? AND kcu.column_name = ?",
                String.class,
                tableName,
                columnName);
    }

    private List<String> indexDefinitions(String tableName) {
        return jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes "
                        + "WHERE schemaname = current_schema() AND tablename = ?",
                String.class,
                tableName);
    }
}
