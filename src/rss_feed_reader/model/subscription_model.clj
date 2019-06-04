(ns rss-feed-reader.model.subscription-model
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [clojure.walk]
            [rss-feed-reader.config.db :as db]
            [rss-feed-reader.model.common :refer :all]))


(s/def ::insert-subscription (s/keys :req-un [::tag ::feed-id ::consumer-id]
                                     :opt-un [::id ::enabled]))

(s/def ::update-skip-null-subscription (s/keys :req-un [::id ::version]
                                               :opt-un [::feed-id ::enabled ::tag ::consumer-id]))

; ==== load

(defn all-enabled
  "Get list of all subscriptions"
  []
  (log/info "getting all subscriptions")
  (into [] (sql/query (db/db-connection) ["select * from subscription where enabled = true order by insert_date desc"])))

(defn by-id
  "Get subscription by id"
  ([id]
   (by-id id (db/db-connection)))
  ([id conn]
   (log/info "loading subscription by id=" id)
   (first (sql/query conn ["select * from subscription where id = (?::uuid)" id]))))

(defn by-tag
  "Get subscription by tag"
  ([tag]
   (by-tag tag (db/db-connection)))
  ([tag sql-connection]
   (log/info "loading subscriptions by tag=" tag)
   (sql/query sql-connection ["select * from subscription where tag = ?" tag])))

(defn batch-by-feed-id
  "Batch load subscriptions by feed ids"
  ([ids]
   (batch-by-feed-id ids (db/db-connection)))
  ([ids sql-connection]
   (log/info "loading subscription by feed ids=" ids)
   (into [] (sql/query sql-connection
                       (to-batch-load-query "select * from subscription where feed_id in (?)" ids)))))

(defn by-feed-id-and-consumer-id
  "Get subscriptions by feed id"
  ([feed-id consumer-id]
   (by-feed-id-and-consumer-id feed-id consumer-id (db/db-connection)))
  ([feed-id consumer-id conn]
   (log/info "loading subscriptions by consumer id= " consumer-id " and feed id= " feed-id)
   (sql/query conn ["select * from subscription where feed_id = (?::uuid) and consumer_id =(?::uuid)" feed-id consumer-id])))

(defn by-consumer-id
  "Get subscriptions by consumer id"
  ([consumer-id]
   (by-consumer-id consumer-id (db/db-connection)))
  ([consumer-id conn]
   (log/info "loading subscriptions by consumer id= " consumer-id)
   (sql/query conn ["select * from subscription where consumer_id = (?::uuid) " consumer-id])))

; ==== insert

(defn insert
  "Insert given subscription if valid"
  ([subscription]
   {:pre [(s/valid? ::insert-subscription subscription)]}
   (insert subscription (db/db-connection)))
  ([subscription conn]
   (let [autocompleted-subscription (autocomplete-insert subscription)]
     (log/info "creating subscription= " autocompleted-subscription)
     (sql/with-db-transaction [conn conn]
                              (sql/execute! conn [(to-sql-insert autocompleted-subscription "subscription" {:id "uuid"})])
                              (by-id (:id autocompleted-subscription) conn)))))

; ==== update

(defn update-skip-null
  "Update subscription"
  ([subscription]
   {:pre [(s/valid? ::update-skip-null-subscription subscription)]}
   (update-skip-null subscription (db/db-connection)))
  ([subscription conn]
   (let [autocompleted-subscription (autocomplete-update subscription)]
     (log/info "updating subscription= " autocompleted-subscription)
     (sql/with-db-transaction [conn conn]
                              (sql/execute! conn [(to-sql-update-skip-null autocompleted-subscription "subscription" {:id "uuid"} :id)])
                              (by-id (:id autocompleted-subscription) conn)))))