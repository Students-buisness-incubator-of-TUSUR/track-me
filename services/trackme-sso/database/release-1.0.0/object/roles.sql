-- liquibase formatted sql

-- changeset akarmanov:roles-1
CREATE TABLE sso.roles
(
    role_id               uuid                        NOT NULL DEFAULT uuid_generate_v4(),
    role_code             VARCHAR(100)                NOT NULL,
    role_name             VARCHAR(100)                NOT NULL,

    active                BOOLEAN                     NOT NULL DEFAULT true,
    created_by            VARCHAR(50)                 NOT NULL DEFAULT 'system',
    created_date          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_by       VARCHAR(50)                 NOT NULL DEFAULT 'system',
    last_updated_date     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT current_timestamp,
    object_version_number INTEGER                     NOT NULL DEFAULT 0,

    constraint roles_pk primary key (role_id)
);

