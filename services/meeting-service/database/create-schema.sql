--liquibase formatted sql

--changeset akarmanov:schemas-1
--preconditions onFail:MARK_RAN onError:MARK_RAN
CREATE SCHEMA IF NOT EXISTS meeting_service;
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'meeting_service';

--rollback DROP SCHEMA IF EXISTS meeting_service CASCADE;