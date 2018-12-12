create table subscription (
  id          uuid primary key,
  url         text unique not null,
  insert_date timestamp   not null,
  update_date timestamp default null,
  version     bigint    default 0
);
--;;
create index subscription_url_index
  on subscription (url);