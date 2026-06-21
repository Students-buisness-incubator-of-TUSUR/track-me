--liquibase formatted sql

-- changeset akarmanov:roles-data-1
INSERT INTO sso.roles(role_code, role_name)
VALUES ('USER', 'Пользователь'),
       ('TRACKER', 'Трекер'),
       ('ADMIN', 'Администратор'),
       ('SUPER_ADMIN', 'Супер администратор');