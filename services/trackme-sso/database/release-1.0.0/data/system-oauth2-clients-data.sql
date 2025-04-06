--liquibase formatted sql

--changeSet akarmanov:system-oauth2-clients-data-01
INSERT INTO sso.system_oauth2_clients(client_id, client_secret,
                                      client_secret_expires_at,
                                      client_name, client_authentication_methods,
                                      authorization_grant_types, redirect_uris,
                                      scopes, client_settings, token_settings)
VALUES ('test-client', '$2a$10$UtM.Sk7Kit4XO9oZLomiXeQ3.Z1AV/u0fBPpyjJ0z9EAEUzW5PR/S',
        to_timestamp('2072-01-01', 'YYYY-MM-DD'), 'Тестовый клиент системы',
        'client_secret_basic', 'authorization_code,refresh_token',
        'http://localhost:8080/code', 'read.scope,write.scope', null, null);
