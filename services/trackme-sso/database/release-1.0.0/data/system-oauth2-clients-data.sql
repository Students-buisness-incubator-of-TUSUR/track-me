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


--changeSet akarmanov:system-oauth2-clients-data-02
-- client_secret = swagger-client
INSERT INTO sso.system_oauth2_clients(client_id, client_secret,
                                      client_secret_expires_at,
                                      client_name, client_authentication_methods,
                                      authorization_grant_types, redirect_uris,
                                      post_logout_redirect_uris,
                                      scopes, client_settings, token_settings)
VALUES ('swagger-client', '$2a$10$G2AzJ0wujkRRbyU0hNWoNewpAfSPhg.cmo7HMT8cX9Xe6vdwmTMTW',
        to_timestamp('2072-01-01', 'YYYY-MM-DD'), 'Клиент для Swagger UI',
        'client_secret_post', 'authorization_code,refresh_token,client_credentials',
        'http://localhost:9000/swagger-ui/oauth2-redirect.html', 'http://localhost:9000',
        'read.scope,write.scope', null,
        null);

--changeSet akarmanov:system-oauth2-clients-data-03
INSERT INTO sso.system_oauth2_clients(client_id, client_secret,
                                      client_secret_expires_at,
                                      client_name, client_authentication_methods,
                                      authorization_grant_types, redirect_uris,
                                      post_logout_redirect_uris,
                                      scopes, client_settings, token_settings)
VALUES ('track-me-client', '$2a$12$MauKt/v9PcELn2RbIktW0etKpx9KxUHnnZWRMLG/Tk2RzMMEwkEN.',
        to_timestamp('2072-01-01', 'YYYY-MM-DD'), 'Track Me Client',
        'client_secret_basic', 'authorization_code,client_credentials,refresh_token',
        'http://127.0.0.1:8081/login/oauth2/code/track-me-client', 'http://127.0.0.1:8081',
        'openid,profile',
        null, null);
