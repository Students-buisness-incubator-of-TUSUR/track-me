--liquibase formatted sql
--changeset trackme:yandex-identity
ALTER TABLE sso.users ADD COLUMN yandex_id varchar(128);
ALTER TABLE sso.users ADD CONSTRAINT users_yandex_id_unique UNIQUE (yandex_id);
--rollback ALTER TABLE sso.users DROP COLUMN yandex_id;
