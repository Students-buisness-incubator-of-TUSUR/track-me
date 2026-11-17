--liquibase formatted sql

--changeset akarmanov:overlord-user-1
-- Владелец платформы. Пароль совпадает с паролем пользователя superadmin
-- и должен быть изменён сразу после первого развёртывания.
INSERT INTO sso.users(email, username, password_hash, full_name, active, account_non_locked,
                      organization_id)
SELECT 'overlord@trackme.local', 'overlord',
       '$2a$10$JJMhSB/r/TSEs2OATa4bmeitB7BQhsp1wOm08YZKp6isYB21y3yLW',
       'Владелец платформы', true, true, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sso.users WHERE username = 'overlord'
);

--changeset akarmanov:overlord-user-2
INSERT INTO sso.user_roles(user_id, role_id)
SELECT u.user_id, r.role_id
FROM sso.users u
INNER JOIN sso.roles r ON r.role_code = 'OVERLORD'
LEFT JOIN sso.user_roles ur ON u.user_id = ur.user_id AND ur.role_id = r.role_id
WHERE u.username = 'overlord'
  AND ur.user_id IS NULL;
