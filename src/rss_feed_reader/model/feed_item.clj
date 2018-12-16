(ns rss-feed-reader.model.feed-item
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.model.db :as db]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [cheshire.core :refer :all]
            [rss-feed-reader.model.auto-complete :as auto-complete]))

(s/def ::insert-feed-item (s/keys :req-un [::subscription-id ::hash ::item]
                                  :opt-un [::id ::insert_date ::version ::update_date]))

; ==== load

(defn by-id
  ([id]
   (by-id id (db/db-connection)))
  ([id connection]
   (log/info "loading feed item by id=" id)
   (sql/query connection ["select * from feed_item where id = (?::uuid)" id])))

(defn by-subscription-id
  "load feed items by subscription id"
  ([subscription-id]
   (by-subscription-id subscription-id (db/db-connection)))
  ([subscription-id connection]
   (log/info "loading feed items by subscription id")
   (into [] (sql/query connection
                       ["select * from feed_item where subscription_id = (?::uuid)" subscription-id]))))

(defn by-hash
  "load feed items by hash"
  ([hash]
   (by-hash hash (db/db-connection)))
  ([hash connection]
   (log/info "loading feed items by hash=" hash)
   (sql/query connection ["select * from feed_item where hash = ?" hash])))

; ==== insert

(defn autocomplete-feed-item-insert
  "auto complete missing field of subscription"
  [feed-item]
  (-> feed-item
      (auto-complete/autocomplete-id)
      (auto-complete/autocomplete-insert-date)
      (auto-complete/autocomplete-version)))

(defn insert
  "insert feed item"
  [feed-item]
  {:pre [(s/valid? ::insert-feed-item feed-item)]}
  (let [autocompleted-feed-item (autocomplete-feed-item-insert feed-item)]
    (log/info "creating feed-item=" autocompleted-feed-item)
    (sql/with-db-transaction [conn (db/db-connection)]
                             (sql/execute! conn ["insert into feed_item values((?::uuid),(?::uuid),(?::json),?,?,?,?)"
                                                 (:id autocompleted-feed-item)
                                                 (:subscription-id autocompleted-feed-item)
                                                 (:item autocompleted-feed-item)
                                                 (:hash autocompleted-feed-item)
                                                 (:insert_date autocompleted-feed-item)
                                                 (:update_date autocompleted-feed-item)
                                                 (:version autocompleted-feed-item)])
                             (by-id (:id autocompleted-feed-item) conn))))