--liquibase formatted sql

--changeset popovam:schemas-1
--preconditions onFail:MARK_RAN onError:MARK_RAN
CREATE SCHEMA IF NOT EXISTS telegram_service;
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'telegram_service'