package com.adtech.iaterap.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI-194 / T8-SH: verifies the partial index used by the reviewer queue.
 *
 * <p>The metadata assertion proves the migration created a genuinely partial
 * index, while the query-plan assertion proves PostgreSQL can select it for
 * the canonical IN_REVIEW lookup. Sequential scans are disabled only while
 * explaining this intentionally small synthetic table; otherwise PostgreSQL
 * may correctly choose a sequential scan because the demo dataset is tiny.</p>
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class ReviewQueueIndexVerificationTest {

    private static final String INDEX_NAME = "idx_test_procedures_in_review";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesPartialIndexForInReviewRows() {
        String indexDefinition = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes "
                        + "WHERE schemaname = current_schema() "
                        + "AND tablename = 'test_procedures' "
                        + "AND indexname = ?",
                String.class,
                INDEX_NAME);

        String normalized = indexDefinition.toLowerCase(Locale.ROOT);
        assertTrue(normalized.contains("where") && normalized.contains("approval_status")
                        && normalized.contains("'in_review'"),
                "Expected a partial predicate restricted to IN_REVIEW: " + indexDefinition);
    }

    @Test
    void inReviewQueueQueryCanUseThePartialIndex() {
        jdbcTemplate.execute("SET enable_seqscan = off");
        try {
            List<String> plan = jdbcTemplate.queryForList(
                    "EXPLAIN (COSTS OFF) "
                            + "SELECT id FROM test_procedures "
                            + "WHERE approval_status = 'IN_REVIEW'",
                    String.class);

            assertTrue(plan.stream().anyMatch(line -> line.contains(INDEX_NAME)),
                    "Expected the IN_REVIEW query plan to use " + INDEX_NAME + ": " + plan);
        } finally {
            jdbcTemplate.execute("RESET enable_seqscan");
        }
    }
}
