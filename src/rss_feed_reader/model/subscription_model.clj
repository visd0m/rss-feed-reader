(ns rss-feed-reader.model.subscription-model
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [clojure.walk]
            [rss-feed-reader.config.db :as db]
            [rss-feed-reader.model.common :refer :all]))


(s/def ::insert-subscription (s/keys :req-un [::url ::tag]
                                     :opt-un [::id ::enabled]))

(s/def ::update-skip-null-subscription (s/keys :req-un [::id ::version]
                                               :opt-un [::url ::enabled ::tag]))

; ==== load

(defn all
  "get list of all subscriptions"
  []
  (log/info "getting all subscriptions")
  (into [] (sql/query (db/db-connection) ["select * from subscription where enabled = true order by insert_date desc"])))

; todo implement batch load

(defn by-id
  "get subscription by id"
  ([id]
   (by-id id (db/db-connection)))
  ([id conn]
   (log/info "loading subscription by id=" id)
   (first (sql/query conn ["select * from subscription where id = (?::uuid)" id]))))

(defn by-url
  "get subscriptions by url"
  ([url]
   (by-url url (db/db-connection)))
  ([url conn]
   (log/info "loading subscriptions by url=" url)
   (sql/query conn ["select * from subscription where url = ?" url])))

; ==== insert

(defn insert
  "insert given subscription if valid"
  ([subscription]
   {:pre [(s/valid? ::insert-subscription subscription)]}
   (insert subscription (db/db-connection)))
  ([subscription conn]
   (let [autocompleted-subscription (autocomplete-insert subscription)]
     (log/info "creating subscription=" autocompleted-subscription)
     (sql/with-db-transaction [conn (db/db-connection)]
                              (sql/execute! conn [(to-sql-insert autocompleted-subscription "subscription" {:id "uuid"})])
                              (by-id (:id autocompleted-subscription) conn)))))


; ==== update

(defn update-skip-null
  "update subscription"
  ([subscription]
   {:pre [(s/valid? ::update-skip-null-subscription subscription)]}
   (update-skip-null subscription (db/db-connection)))
  ([subscription conn]
   (let [autocompleted-subscription (autocomplete-update subscription)]
     (log/info "updating subscription=" autocompleted-subscription)
     (sql/with-db-transaction [conn (db/db-connection)]
                              (sql/execute! conn [(to-sql-update-skip-null autocompleted-subscription "subscription" {:id "uuid"} :id)])
                              (by-id (:id autocompleted-subscription) conn)))))