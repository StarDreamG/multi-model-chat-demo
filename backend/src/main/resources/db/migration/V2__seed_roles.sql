INSERT INTO sys_role (role_code, role_name)
VALUES ('ADMIN', 'Administrator'), ('USER', 'Normal User')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);
