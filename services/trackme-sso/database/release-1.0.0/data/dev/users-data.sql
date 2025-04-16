--liquibase formatted sql

--changeset akarmanov:users-data.sql-1
INSERT INTO sso.users(email, username, password_hash, full_name, avatar_url, phone_number)
VALUES ('admin@admin.com', 'admin',
        '$2a$10$VqZNeKOn56/QFbAZ5LWAwemLnhUzBbPw81KcFGMgTIQlacwNH/CO6', 'Администратор', null,
        '1234567890');

--changeset akarmanov:users-data.sql-2
INSERT INTO sso.users(email, username, password_hash, full_name, avatar_url, phone_number)
VALUES ('tracker@tracker.com', 'tracker',
        '$2a$10$D06CV65LyR0UAoOIeYWaUODslneCMmPdKai8lkYNolOJJQyhdIvoC', 'Трекер', null,
        '1234567890');


--changeset akarmanov:users-data.sql-3
INSERT INTO sso.user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM sso.users WHERE username = 'admin'),
        (SELECT role_id FROM sso.roles WHERE role_code = 'ADMIN'));

--changeset akarmanov:users-data.sql-4
INSERT INTO sso.user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM sso.users WHERE username = 'tracker'),
        (SELECT role_id FROM sso.roles WHERE role_code = 'TRACKER'));