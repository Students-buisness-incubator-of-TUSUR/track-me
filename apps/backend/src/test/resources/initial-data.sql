INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id, role, enabled, email)
VALUES (RANDOM_UUID(), 'test_tracker', '$2a$10$gUvgLfFQbY00Vp90DDhjgeT5zsZr2i6ZYsim2K0/REulhZDrVHrr.W',
        'test_tracker', 'test_tracker', 'test_tracker',
        'test_tracker', 'TRACKER', true, '');

INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id, role, enabled, email)
VALUES (RANDOM_UUID(), 'test_admin', '$2a$10$gUvgLfFQbY00Vp90DDhjgeT5zsZr2i6ZYsim3K0/REulhZDrVHrr.W',
        'test_admin', 'test_admin', 'test_admin',
        'test_admin', 'ADMIN', true, '');

INSERT INTO pp_user (id, first_name, password, last_name, middle_name, phone_number, telegram_id, role, enabled, email)
VALUES (RANDOM_UUID(), 'test_super_admin', '$2a$10$gUvgLfFQbY00Vp90DDhjgeT5zsZr2i6ZYsimK0/REulh4ZDrVHrr.W',
        'test_super_admin', 'test_super_admin', 'test_super_admin',
        'test_super_admin', 'SUPER_ADMIN', true, '');
