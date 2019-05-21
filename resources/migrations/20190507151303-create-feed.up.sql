create table feed
(
  id           uuid primary key,
  url          text      not null,
  enabled      boolean   default true,
  order_unique bigint    default null,
  insert_date  timestamp not null,
  update_date  timestamp default null,
  version      bigint    default 0
);
--;;
create index feed_url_index
  on feed (url);
--;;
create index feed_order_unique_index
  on feed (order_unique);