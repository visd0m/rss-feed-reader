alter table feed_item
    add column subscription_id uuid null;
--;;
create index feed_item_subscription_id_index on feed_item (subscription_id);