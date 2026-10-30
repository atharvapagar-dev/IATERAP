-- AI-272: audit evidence is append-only for the least-privileged runtime role.
-- The deployment DBA provisions LOGIN role iaterap_app and configures the
-- backend datasource with it; Flyway's migration owner remains separate.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'iaterap_app') THEN
        GRANT USAGE ON SCHEMA public TO iaterap_app;
        GRANT SELECT, INSERT ON TABLE audit_log TO iaterap_app;
        GRANT USAGE, SELECT ON SEQUENCE audit_log_id_seq TO iaterap_app;
        REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_log FROM iaterap_app;
    END IF;
END $$;

REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_log FROM PUBLIC;
