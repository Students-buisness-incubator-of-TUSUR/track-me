--liquibase formatted sql

--changeset akarmanov:organizations-data-1
INSERT INTO sso.organizations(organization_id, name, status, expiration_date)
SELECT '00000000-0000-0000-0000-000000000001', 'Организация по умолчанию', 'ACTIVE', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sso.organizations WHERE organization_id = '00000000-0000-0000-0000-000000000001'
);
