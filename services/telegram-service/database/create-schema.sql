--liquibase formatted sql

CREATE SCHEMA IF NOT EXISTS telegram_service;

--rollback DROP SCHEMA IF EXISTS telegram_service CASCADE;