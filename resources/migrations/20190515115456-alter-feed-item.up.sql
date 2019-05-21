alter table feed_item
  add column feed_id uuid null;
--;;
create index feed_item_feed_id_index
  on feed_item (feed_id);