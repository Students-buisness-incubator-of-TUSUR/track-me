create table authorities
(
    telegram_id varchar(50) not null,
    authority   varchar(50) not null,
    constraint fk_authorities_users foreign key (telegram_id) references pp_user (telegram_id)
);
create unique index ix_auth_username on authorities (telegram_id, authority);