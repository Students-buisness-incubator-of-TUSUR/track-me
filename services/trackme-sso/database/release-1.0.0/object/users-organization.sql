--liquibase formatted sql

--changeset akarmanov:users-organization-1
ALTER TABLE sso.users
    ADD COLUMN IF NOT EXISTS organization_id UUID DEFAULT '00000000-0000-0000-0000-000000000001';
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'sso' AND table_name = 'users' AND column_name = 'organization_id'

--changeset akarmanov:users-organization-2
UPDATE sso.users
SET organization_id = '00000000-0000-0000-0000-000000000001'
WHERE organization_id IS NULL;

--changeset akarmanov:users-organization-3
ALTER TABLE sso.users
    ADD CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES sso.organizations (organization_id);
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'sso' AND table_name = 'users' AND constraint_name = 'fk_users_organization'

--changeset akarmanov:users-organization-4
CREATE INDEX IF NOT EXISTS idx_users_organization_id
    ON sso.users (organization_id);
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'sso' AND indexname = 'idx_users_organization_id'

COMMENT ON COLUMN sso.users.organization_id IS 'Организация, к которой относится пользователь';
