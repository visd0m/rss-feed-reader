create table consumer
(
  id           uuid primary key,
  name         text      null,
  surname      text      null,
  phone_number text      null,
  external_id  text      null,
  order_unique bigint    default null,
  insert_date  timestamp not null,
  update_date  timestamp default null,
  version      bigint    default 0
);
--;;
create index consumer_external_id_index
  on consumer (external_id);
--;;
create index consumer_order_unique_index
  on consumer (order_unique);