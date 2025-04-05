create table if not exists users
(
    username                varchar(50)  not null
        primary key,
    password                varchar(255) not null,
    enabled                 boolean      not null,
    account_non_expired     boolean      not null,
    account_non_locked      boolean      not null,
    credentials_non_expired boolean      not null
);

create table if not exists authorities
(
    username  varchar(50) not null
        constraint fk_authorities_users
            references users,
    authority varchar(50) not null
);

create unique index if not exists ix_auth_username
    on authorities (username, authority);

