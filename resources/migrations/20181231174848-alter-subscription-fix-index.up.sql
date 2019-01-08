alter table subscription
  add column order_unique bigint default null;
--;;
create index subscription_order_unique_index
  on subscription (order_unique);
--;;
drop index feed_item_order_unique_index;
--;;
create index feed_item_order_unique_index
  on feed_item (order_unique);