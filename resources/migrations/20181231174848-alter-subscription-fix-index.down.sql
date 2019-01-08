alter table subscription
  drop column order_unique;
--;;
drop index subscription_order_unique_index;