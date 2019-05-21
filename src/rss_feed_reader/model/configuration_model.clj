(ns rss-feed-reader.model.configuration-model
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [rss-feed-reader.config.db :as db]
            [rss-feed-reader.model.common :refer :all]))


(s/def ::put-configuration (s/keys :req-un [::key ::value]))

; ==== get key

(defn get-key
  "Get configuration"
  ([key]
   (get-key key (db/db-connection)))
  ([key sql-connection]
   (let [result (first (sql/query sql-connection ["select * from configuration where key = ?" key]))]
     (log/info "loaded configuration=" result)
     result)))

(defn- insert
  ([configuration]
   (insert configuration (db/db-connection)))
  ([configuration sql-connection]
   (let [entity-to-insert (-> configuration
                              (autocomplete-insert-date)
                              (set-or-increment-version))]
     (log/info "inserting configuration=" entity-to-insert)
     (sql/with-db-transaction [sql-connection sql-connection]
                              (sql/execute! sql-connection [(to-sql-insert entity-to-insert "configuration" {})])
                              (get-key (:key configuration) sql-connection)))))

(defn- update
  ([configuration]
   (update configuration (db/db-connection)))
  ([configuration sql-connection]
   (let [entity-to-update (-> configuration
                              (autocomplete-update-date)
                              (set-or-increment-version))]
     (log/info "updating configuration=" entity-to-update)
     (sql/with-db-connection [sql-connection sql-connection]
                             (sql/execute! sql-connection [(to-sql-update-skip-null entity-to-update "configuration" {} :key)])
                             (get-key (:key configuration) sql-connection)))))

; == put key

(defn put-key
  "Put configuration"
  ([configuration]
   {:pre [(s/valid? ::put-configuration configuration)]}
   (put-key configuration (db/db-connection)))
  ([configuration sql-connection]
   (sql/with-db-transaction [sql-connection sql-connection]
                            (if-let [config (get-key (:key configuration) sql-connection)]
                              (let [c (assoc configuration :version (:version config))]
                                (log/info c)
                                (update c sql-connection))
                              (insert configuration sql-connection)))))
