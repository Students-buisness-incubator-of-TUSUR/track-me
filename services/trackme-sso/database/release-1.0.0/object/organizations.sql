--liquibase formatted sql

--changeset akarmanov:organizations-1
CREATE TABLE IF NOT EXISTS sso.organizations
(
    organization_id       UUID                        NOT NULL DEFAULT gen_random_uuid(),
    name                  VARCHAR(255)                NOT NULL,
    status                VARCHAR(32)                 NOT NULL DEFAULT 'ACTIVE',
    expiration_date       DATE,

    created_by            VARCHAR(50)                 NOT NULL DEFAULT 'system',
    created_date          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_by       VARCHAR(50)                 NOT NULL DEFAULT 'system',
    last_updated_date     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT current_timestamp,
    object_version_number INTEGER                     NOT NULL DEFAULT 0,
    constraint organizations_pk PRIMARY KEY (organization_id)
);
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'organizations' AND table_schema = 'sso'

--changeset akarmanov:organizations-2
CREATE UNIQUE INDEX IF NOT EXISTS idx_organizations_name
    ON sso.organizations (lower(name));
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'sso' AND indexname = 'idx_organizations_name'

COMMENT ON TABLE sso.organizations IS 'Организации (тенанты)';
COMMENT ON COLUMN sso.organizations.organization_id IS 'Идентификатор организации';
COMMENT ON COLUMN sso.organizations.name IS 'Название организации';
COMMENT ON COLUMN sso.organizations.status IS 'Статус организации: ACTIVE или BLOCKED';
COMMENT ON COLUMN sso.organizations.expiration_date IS 'Дата окончания действия организации, NULL — бессрочно';
COMMENT ON COLUMN sso.organizations.created_by IS 'Логин пользователя, создавшего запись';
COMMENT ON COLUMN sso.organizations.created_date IS 'Дата создания записи';
COMMENT ON COLUMN sso.organizations.last_updated_by IS 'Логин пользователя, изменившего запись';
COMMENT ON COLUMN sso.organizations.last_updated_date IS 'Дата последнего обновления записи';
COMMENT ON COLUMN sso.organizations.object_version_number IS 'Номер версии записи в БД';
