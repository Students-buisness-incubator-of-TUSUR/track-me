--liquibase formatted sql

-- changeset akarmanov:users-data-1
INSERT INTO sso.users(email, username, password_hash, full_name, avatar_url, active)
VALUES ('', 'superadmin', '$2a$10$JJMhSB/r/TSEs2OATa4bmeitB7BQhsp1wOm08YZKp6isYB21y3yLW',
        'Суперадминистратор', '', true);

-- changeset akarmanov:users-data-2
INSERT INTO sso.user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM sso.users WHERE username = 'superadmin'),
        (SELECT role_id FROM sso.roles WHERE role_code = 'SUPER_ADMIN'));