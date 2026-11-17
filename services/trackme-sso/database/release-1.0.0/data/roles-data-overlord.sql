--liquibase formatted sql

-- changeset akarmanov:roles-data-overlord-1
INSERT INTO sso.roles(role_code, role_name)
SELECT 'OVERLORD', 'Владелец платформы'
WHERE NOT EXISTS (
    SELECT 1 FROM sso.roles WHERE role_code = 'OVERLORD'
);
