(ns rss-feed-reader.model.feed-item-model
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.config.db :as db]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [cheshire.core :refer :all]
            [rss-feed-reader.model.common :refer :all]))

(s/def ::insert-feed-item (s/keys :req-un [::subscription-id ::hash ::item]
                                  :opt-un [::id ::insert-date ::version ::update-date ::order-unique]))

; ==== load

(defn by-id
  ([id]
   (by-id id (db/db-connection)))
  ([id connection]
   (log/info "loading feed item by id=" id)
   (sql/query connection ["select * from feed_item where id = (?::uuid)" id])))

(defn by-subscription-id
  "load feed items by subscription id paginating over order-unique"
  ([subscription-id starting-after limit]
   (by-subscription-id subscription-id starting-after limit (db/db-connection)))
  ([subscription-id starting-after limit connection]
   (log/info "loading feed items by subscription id=" subscription-id " starting after=" starting-after " limit=" limit)
   (into [] (map #(assoc % :item (cheshire.core/parse-string (:value (bean (:item %)))))
                 (let [query (str "select *
                              from feed_item
                              where subscription_id = (?::uuid)"
                                  (if starting-after (str "and order_unique < " starting-after)) ""
                                  "order by order_unique desc limit ?")]
                   (sql/query connection [query subscription-id (int limit)]))))))

(defn by-hash
  "load feed items by hash"
  ([hash]
   (by-hash hash (db/db-connection)))
  ([hash connection]
   (log/info "loading feed items by hash=" hash)
   (sql/query connection ["select * from feed_item where hash = ?" hash])))

; ==== insert

(defn insert
  "insert feed item"
  ([feed-item]
   {:pre [(s/valid? ::insert-feed-item feed-item)]}
   (insert feed-item (db/db-connection)))
  ([feed-item conn]
   (let [autocompleted-feed-item (autocomplete-insert feed-item)]
     (log/info "creating feed-item=" autocompleted-feed-item)
     (sql/with-db-transaction [conn (db/db-connection)]
                              (sql/execute! conn [(entity->sql-insert autocompleted-feed-item "feed_item" {:id              "uuid"
                                                                                                           :subscription-id "uuid"
                                                                                                           :item            "json"})])
                              (by-id (:id autocompleted-feed-item) conn)))))