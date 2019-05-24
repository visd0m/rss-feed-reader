(ns rss-feed-reader.model.consumer-model
  (:require [clojure.spec.alpha :as s]
            [rss-feed-reader.config.db :as db]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [rss-feed-reader.model.common :refer :all]))

(s/def ::insert-consumer (s/keys :req-un [::external-id]
                                 :opt-un [::id ::name ::surname ::phone-number ::username]))

(s/def ::update-skip-null-consumer (s/keys :req-un [::id ::version]
                                           :opt-un [::name ::surname ::phone-number ::external-id ::username]))

; ==== load

(defn by-id
  "Get consumer by id"
  ([id]
   (by-id id (db/db-connection)))
  ([id conn]
   (log/info "loading consumer by id=" id)
   (first (sql/query conn ["select * from consumer where id = (?::uuid)" id]))))

(defn by-external-id
  "Get consumer by id"
  ([id]
   (by-external-id id (db/db-connection)))
  ([id conn]
   (log/info "loading consumer by id=" id)
   (first (sql/query conn ["select * from consumer where external_id = (?::text)" id]))))

(defn by-external-id-enabled
  "Get consumer by id"
  ([id]
   (by-external-id-enabled id (db/db-connection)))
  ([id conn]
   (log/info "loading consumer by id=" id)
   (first (sql/query conn ["select * from consumer where external_id = (?::text) and enabled = true" id]))))

; ==== insert

(defn insert
  "Insert given consumer if valid"
  ([consumer]
   {:pre [(s/valid? ::insert-consumer consumer)]}
   (insert consumer (db/db-connection)))
  ([consumer conn]
   (let [autocompleted-consumer (autocomplete-insert consumer)]
     (log/info "creating consumer=" autocompleted-consumer)
     (sql/with-db-transaction [conn conn]
                              (sql/execute! conn [(to-sql-insert autocompleted-consumer "consumer" {:id "uuid"})])
                              (by-id (:id autocompleted-consumer) conn)))))

; ==== update

(defn update-skip-null
  "Update consumer"
  ([consumer]
   {:pre [(s/valid? ::update-skip-null-consumer consumer)]}
   (update-skip-null consumer (db/db-connection)))
  ([subscription conn]
   (let [autocompleted-consumer (autocomplete-update subscription)]
     (log/info "updating consumer= " autocompleted-consumer)
     (sql/with-db-transaction [conn (db/db-connection)]
                              (sql/execute! conn [(to-sql-update-skip-null autocompleted-consumer " consumer " {:id " uuid "} :id)])
                              (by-id (:id autocompleted-consumer) conn)))))