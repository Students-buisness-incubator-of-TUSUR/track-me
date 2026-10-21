--liquibase formatted sql
--changeset codex:google-identity
ALTER TABLE sso.users ADD COLUMN google_id VARCHAR(128);
ALTER TABLE sso.users ADD CONSTRAINT users_google_id_unique UNIQUE (google_id);
--rollback ALTER TABLE sso.users DROP COLUMN google_id;
