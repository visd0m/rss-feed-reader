create table configuration
(
    key         text primary key not null,
    value       text             not null,
    insert_date timestamp        not null,
    update_date timestamp default null,
    version     bigint    default 0
);