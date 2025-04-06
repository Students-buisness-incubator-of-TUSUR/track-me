--liquibase formatted sql

--changeset akarmanov:extensions-1
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";