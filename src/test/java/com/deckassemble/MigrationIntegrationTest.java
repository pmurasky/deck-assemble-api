package com.deckassemble;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyInteroperabilityAnalyticsMigration() {
        assertThat(countAppliedRelease()).isOne();
        assertThat(tableExists("deck_import_previews")).isTrue();
        assertThat(tableExists("collection_import_previews")).isTrue();
        assertThat(hasIdempotencyConstraint("deck_import_previews")).isTrue();
        assertThat(hasIdempotencyConstraint("collection_import_previews")).isTrue();
    }

    private int countAppliedRelease() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM databasechangelog WHERE id = '012-interoperability-analytics'",
                Integer.class);
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        "SELECT to_regclass('public.' || ?) IS NOT NULL",
                        Boolean.class,
                        tableName));
    }

    private boolean hasIdempotencyConstraint(String tableName) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*) = 1
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        WHERE t.relname = ?
                          AND c.contype = 'u'
                          AND pg_get_constraintdef(c.oid) = 'UNIQUE (profile_id, idempotency_key)'
                        """,
                        Boolean.class,
                        tableName));
    }
}
