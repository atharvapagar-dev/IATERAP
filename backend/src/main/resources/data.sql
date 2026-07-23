INSERT INTO users (username, email, password, role)
VALUES
('admin', 'admin@iaterap.com', 'admin123', 'ADMIN'),
('syseng', 'sys@iaterap.com', 'sys123', 'SYSTEM_ENGINEER'),
('reliability', 'rel@iaterap.com', 'rel123', 'RELIABILITY_ENGINEER'),
('tester', 'test@iaterap.com', 'test123', 'TEST_ENGINEER'),
('field', 'field@iaterap.com', 'field123', 'FIELD_ENGINEER'),
('viewer', 'view@iaterap.com', 'view123', 'VIEWER');

INSERT INTO project (name, description)
VALUES
('IATERAP Demo Aircraft', 'Top-level project for the demo aircraft management flow.'),
('Reliability Validation Program', 'Program used for subsystem and LRU tracking.');
/*-- Initial Admin User
INSERT INTO users ...

-- Future Roles:
-- ADMIN
-- SYSTEM_ENGINEER
-- RELIABILITY_ENGINEER
-- TEST_ENGINEER
-- FIELD_ENGINEER
-- VIEWER*/
