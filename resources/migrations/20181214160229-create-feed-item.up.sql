create table feed_item (
  id              uuid primary key,
  subscription_id uuid   not null,
  item            jsonb,
  hash            text   not null unique,
  order_unique    bigint not null,
  insert_date     timestamp,
  update_date     timestamp,
  version         bigint default 0
);
--;;
create index feed_item_subscription_id_index
  on feed_item (subscription_id);
--;;
create index feed_item_hash_index
  on feed_item (hash);
--;;
create index feed_item_order_unique_index
  on feed_item (hash);