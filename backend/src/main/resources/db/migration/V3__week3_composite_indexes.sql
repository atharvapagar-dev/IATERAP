CREATE INDEX IF NOT EXISTS idx_signal_libraries_lru_interface
    ON signal_libraries (lru_id, interface_id);

CREATE INDEX IF NOT EXISTS idx_signal_ranges_library_severity
    ON signal_ranges (signal_library_id, severity);

CREATE INDEX IF NOT EXISTS idx_protocol_tags_lru_name
    ON protocol_tags (lru_id, name);
