--liquibase formatted sql

--changeset akarmanov:ronin-user-1
-- Вставляем пользователя 'ronin' с ролью TRACKER
-- Миграция идемпотентна - использует WHERE NOT EXISTS для защиты от дубликатов
-- Может запускаться многократно без ошибок (например, при rollback/reapply)
INSERT INTO sso.users(email, username, password_hash, full_name, active, account_non_locked)
SELECT 'ronin@ronin.com', 'ronin',
       '$2a$10$iHkWzHmHkqY1EkJFNJdTZezEJH7ZCv8mLkoybRNxaW8fFbxDmJqJe',
       'Ронин', false, true
WHERE NOT EXISTS (
  SELECT 1 FROM sso.users WHERE username = 'ronin'
);

--changeset akarmanov:ronin-user-2
-- Вставляем роль TRACKER для пользователя 'ronin'
-- LEFT JOIN эффективнее, чем NOT EXISTS для больших таблиц
-- Гарантирует отсутствие дубликатов при повторных запусках
INSERT INTO sso.user_roles(user_id, role_id)
SELECT u.user_id, r.role_id
FROM sso.users u
INNER JOIN sso.roles r ON r.role_code = 'TRACKER'
LEFT JOIN sso.user_roles ur ON u.user_id = ur.user_id AND ur.role_id = r.role_id
WHERE u.username = 'ronin' 
  AND ur.user_id IS NULL;