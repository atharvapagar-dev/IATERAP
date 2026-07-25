-- The V1 baseline seeded demo users with plaintext passwords. AuthService now
-- verifies credentials with BCryptPasswordEncoder, so those stored values must
-- be BCrypt hashes instead of plaintext. This migration replaces the known
-- plaintext seed passwords with their BCrypt equivalents (same plaintext
-- password each user was seeded with), so the existing demo accounts keep
-- working while actually being validated correctly.
--
-- admin        -> admin123
-- syseng       -> sys123
-- reliability  -> rel123
-- tester       -> test123
-- field        -> field123
-- viewer       -> view123

UPDATE users SET password = '$2b$10$Zjok0aL8xwmpSBUpikY3OOijNIlNYhqleduKh.1PFWZw2KFPcr9cu'
WHERE username = 'admin' AND password = 'admin123';

UPDATE users SET password = '$2b$10$ridYMX/yk5Mh9880ksQ/e.0p7PrfECwk.VUkDDYHTWIDzF0vCyRle'
WHERE username = 'syseng' AND password = 'sys123';

UPDATE users SET password = '$2b$10$evnfchOb896Akd48IjDWJ.vnmucmUF7wa1IXlAMf.RtTwyTF.RjLG'
WHERE username = 'reliability' AND password = 'rel123';

UPDATE users SET password = '$2b$10$DJ1Injm.DpImilISTbIIwOQPep5E.PfVqOM9ISA/hbrJy2JYo.MNO'
WHERE username = 'tester' AND password = 'test123';

UPDATE users SET password = '$2b$10$XiKxE1yimim/lqqidcnxzeyubuv4yTCusiL..9ZpmXT8AB4ELfaZ.'
WHERE username = 'field' AND password = 'field123';

UPDATE users SET password = '$2b$10$BLCcmnNJrKQxVGvd9qn9P.2JGV0MRWvDgmktYwW.mtANNBYNccMfS'
WHERE username = 'viewer' AND password = 'view123';
