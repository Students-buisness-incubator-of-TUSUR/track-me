INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id,
                     role, enabled, email, deleted)
VALUES (gen_random_uuid(), 'test_tracker',
        '$2a$10$gUvgLfFQbY00Vp90DDhjgeT5zsZr2i6ZYsim2K0/REtttulhZDrVHrr.W',
        'test_tracker', 'test_tracker', 'test_tracker',
        'test_tracker', 'TRACKER', true, 'test_tracker@test_user.test_user', false);

INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id,
                     role, enabled, email, deleted)
VALUES (gen_random_uuid(), 'test_admin',
        '$2a$10$gUvgLfFQbY00Vp90DDhjgeT5zsZr2qwei6ZYsim3K0/REulhZDrVHrr.W',
        'test_admin', 'test_admin', 'test_admin',
        'test_admin', 'ADMIN', true, 'test_admin@test_user.test_user', false);

INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id,
                     role, enabled, email, deleted)
VALUES (gen_random_uuid(), 'test_super_admin',
        '$2a$10$gUvgLfFQbY00Vp90DDhjgeT5zsZr2i6ZYwersimK0/REulh4ZDrVHrr.W',
        'test_super_admin', 'test_super_admin', 'test_super_admin',
        'test_super_admin', 'SUPER_ADMIN', true, 'test_super_admin@test_user.test_user', false);

INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id,
                     role, enabled, email, full_name, deleted)
VALUES (gen_random_uuid(), 'test_user',
        '$2a$1rtrt0$gUvgLfFQbY00Vp90DDhjgeT5zsZr2i6ZYsim4K0/REulhZDrVHrr.W',
        'test_user', 'test_user', 'test_user',
        'test_user', 'TRACKER', false, 'test_user@test_user.test_user',
        'test_user test_user test_user', false);
