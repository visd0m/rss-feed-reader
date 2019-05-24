(ns rss-feed-reader.model.feed-model
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [rss-feed-reader.config.db :as db]
            [rss-feed-reader.model.common :refer :all]))

(s/def ::insert-feed (s/keys :req-un [::url]
                             :opt-un [::id ::enabled]))

(s/def ::update-skip-null-feed (s/keys :req-un [::id ::version]
                                       :opt-un [::url ::enabled ::insert-date ::update-date]))

;; === load

(defn all-enabled
  "Get list of all enabled feeds"
  []
  (log/info "Getting all feeds")
  (sql/query (db/db-connection) ["select * from feed where enabled = true order by insert_date desc"]))

(defn by-id
  "Get feed by id"
  ([id]
   (by-id id (db/db-connection)))
  ([id sql-connection]
   (log/info "getting feed with id=" id)
   (first (sql/query sql-connection ["select * from feed where id = (?::uuid)" id]))))

(defn batch-by-id
  "Batch load feeds by ids"
  ([ids]
   (batch-by-id ids (db/db-connection)))
  ([ids sql-connection]
   (log/info "loading feeds by ids=" ids)
   (sql/query sql-connection (to-batch-load-query "select * from feed where id in (?)" ids))))

(defn by-url
  "Get feed by url"
  ([url]
   (by-url url (db/db-connection)))
  ([url conn]
   (log/info "loading feed by url=" url)
   (sql/query conn ["select * from feed where url = ?" url])))

;; === insert

(defn insert
  "Insert given feed if valid"
  ([feed]
   {:pre [(s/valid? ::insert-feed feed)]}
   (insert feed (db/db-connection)))
  ([feed sql-connection]
   (let [entity-to-insert (autocomplete-insert feed)]
     (log/info "creating feed= " entity-to-insert)
     (sql/with-db-transaction [sql-connection sql-connection]
                              (sql/execute! sql-connection [(to-sql-insert entity-to-insert "feed" {:id "uuid"})])
                              (by-id (:id entity-to-insert) sql-connection)))))

; ==== update

(defn update-skip-null
  "Update feed"
  ([feed]
   {:pre [(s/valid? ::update-skip-null-feed feed)]}
   (update-skip-null feed (db/db-connection)))
  ([feed sql-connection]
   (let [entity-to-insert (autocomplete-update feed)]
     (log/info "updating entity=" entity-to-insert)
     (sql/with-db-transaction [sql-connection sql-connection]
                              (sql/execute! sql-connection [(to-sql-update-skip-null entity-to-insert "feed" {:id "uuid"} :id)])
                              (by-id (:id entity-to-insert) sql-connection)))))