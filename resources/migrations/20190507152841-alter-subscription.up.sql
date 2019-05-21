alter table subscription
  add column consumer_id uuid null,
  add column feed_id uuid null;
--;;
create index subscription_consumer_id_index on subscription (consumer_id);
--;;
create index subscription_feed_id_index on subscription (feed_id);