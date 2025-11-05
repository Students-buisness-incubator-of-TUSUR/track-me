-- liquibase formatted sql

CREATE TABLE IF NOT EXISTS telegram_service.chats
(
    id                    UUID                        NOT NULL DEFAULT gen_random_uuid(),
    chat_id               BIGINT                      NOT NULL,
    username              VARCHAR(100)                NOT NULL,
    CONSTRAINT chats_pk PRIMARY KEY (id)
);