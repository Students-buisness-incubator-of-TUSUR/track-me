--liquibase formatted sql

--changeset akarmanov:users-data-1

INSERT INTO sso.users (email, username, password_hash, full_name, avatar_url, active)
VALUES ('admin@example.com', 'admin',
        '$2a$10$VUqrcPxSpEhmYjIZ5zbygu3bEf1KHw8A8Vm4agZwh061SVFGr2OUG', 'Иван', null, true);