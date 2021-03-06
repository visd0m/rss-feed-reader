(ns rss-feed-reader.model.feed-item-model
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.config.db :as db]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [cheshire.core :refer :all]
            [rss-feed-reader.model.common :refer :all]))

(s/def ::insert-feed-item (s/keys :req-un [::hash ::item ::feed-id]
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
  ([subscription-id starting-after search limit]
   (by-subscription-id subscription-id starting-after search limit (db/db-connection)))
  ([subscription-id starting-after search limit connection]
   (log/info "loading feed items by subscription id=" subscription-id " starting after=" starting-after " limit=" limit)
   (let [query (str "select * from feed_item where subscription_id = ('" subscription-id "'::uuid)"
                    (reduce (fn [acc entry]
                              (str acc " and item->>'" (key entry) "' ilike '%" (escape-single-quote (val entry)) "%'"))
                            ""
                            (dissoc search "starting_after" "limit"))
                    (if starting-after (str " and order_unique < " starting-after)) ""
                    " order by order_unique desc limit " (int limit))
         result (do (println "[SELECT] " query)
                    (sql/query connection [query]))]
     (into []
           (map #(assoc % :item (cheshire.core/parse-string (:value (bean (:item %))))) result)))))

(defn by-hash
  "load feed items by hash"
  ([hash]
   (by-hash hash (db/db-connection)))
  ([hash connection]
   (log/info "loading feed items by hash=" hash)
   (sql/query connection ["select * from feed_item where hash = ?" hash])))

(defn by-date-after
  ([date]
   (by-date-after date (db/db-connection)))
  ([date connection]
   (log/info "loading feed items after date=" date)
   (let [result (sql/query connection ["select * from feed_item where insert_date > (?::timestamp) order by insert_date asc" date])]
     (into []
           (map #(assoc % :item (cheshire.core/parse-string (:value (bean (:item %))))) result)))))

(defn by-feed-id-and-date-after
  "Load feed items by feed ids"
  ([id date]
   (by-feed-id-and-date-after id date (db/db-connection)))
  ([id date sql-connection]
   (log/info "loading feed items by feed id=" id)
   (let [result (sql/query sql-connection ["select * from feed_item where feed_id = (?::uuid) and insert_date > (?::timestamp) order by insert_date desc" id date])]
     (into []
           (map #(assoc % :item (cheshire.core/parse-string (:value (bean (:item %))))) result)))))

(defn batch-by-feed-id-and-date-after
  "Batch load feed items by feed ids"
  ([ids date]
   (batch-by-feed-id-and-date-after ids date (db/db-connection)))
  ([ids date sql-connection]
   (log/info "loading feed items by feed ids=" ids)
   (let [result (sql/query sql-connection
                           [(str (to-batch-load-query "select * from feed_item where feed_id in (?)" ids)
                                 " and insert_date > (?::timestamp) order by insert_date desc")
                            date])]
     (into []
           (map #(assoc % :item (cheshire.core/parse-string (:value (bean (:item %))))) result)))))


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
                              (sql/execute! conn [(to-sql-insert autocompleted-feed-item "feed_item" {:id              "uuid"
                                                                                                      :subscription-id "uuid"
                                                                                                      :item            "json"})])
                              (by-id (:id autocompleted-feed-item) conn)))))