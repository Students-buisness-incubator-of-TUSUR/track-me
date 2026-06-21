--liquibase formatted sql

--changeset akarmanov:schemas-1
CREATE SCHEMA IF NOT EXISTS sso;
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'sso'