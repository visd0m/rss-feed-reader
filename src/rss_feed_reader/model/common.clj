(ns rss-feed-reader.model.common
  (:require [clojure.tools.logging :as log]))

; == auto complete

(defn autocomplete-id
  "set entity id if missing"
  [entity]
  (if-not (:id entity)
    (assoc entity :id (java.util.UUID/randomUUID))
    entity))

(defn autocomplete-insert-date
  "set entity insert-date if missing"
  [entity]
  (if-not (:insert-date entity)
    (assoc entity :insert-date (java.sql.Timestamp/from (java.time.Instant/now)))
    entity))

(defn autocomplete-update-date
  "set entity update-date"
  [entity]
  (assoc entity :update-date (java.sql.Timestamp/from (java.time.Instant/now))))

(defn set-or-increment-version
  "set entity version if missing or increment it"
  [entity]
  (let [version (:version entity)]
    (if version
      (assoc entity :version (+ version 1))
      (assoc entity :version 0))))

(defn auto-complete-order-unique
  "set entity order unique if missing"
  [entity]
  (if-not (:oder-unique entity)
    (assoc entity :order-unique (.toEpochMilli (java.time.Instant/now)))))

(defn autocomplete-insert
  "auto complete missing field of subscription"
  [entity]
  (-> entity
      (autocomplete-id)
      (autocomplete-insert-date)
      (auto-complete-order-unique)
      (set-or-increment-version)))

(defn autocomplete-update
  "auto complete missing field of subscription"
  [entity]
  (-> entity
      (autocomplete-update-date)
      (set-or-increment-version)))

(defn escape-quotes
  [string-to-escape]
  ; fixme
  (clojure.string/replace string-to-escape "'" ""))

; === insert

(defn entity->sql-insert
  "generate postgres sql insert statement"
  [entity table custom-types-by-key]
  (let [entity-keys (->>
                      (keys entity)
                      (map #(clojure.string/replace (clojure.core/name %) "-" "_"))
                      (clojure.string/join ","))
        values (->>
                 (keys entity)
                 (map #(if-let [type (% custom-types-by-key)]
                         (str "('" (escape-quotes (% entity)) "'" "::" type ")")
                         (str "'" (escape-quotes (% entity)) "'")))
                 (clojure.string/join ","))
        query (str "insert into " table " (" entity-keys ") values (" values ")")]
    (log/info "[INSERT]=" query)
    query))

; === update

(defn- get-sql-update-assignment
  [entity custom-types-by-key key]
  (str (clojure.string/replace (clojure.core/name key) "-" "_") "=" (if-let [type (key custom-types-by-key)]
                                                                      (str "('" (escape-quotes (key entity)) "'" "::" type ")")
                                                                      (str "'" (escape-quotes (key entity)) "'"))))

(defn entity->sql-update-skip-null
  "generate update skip null postgres sql statement"
  [entity table custom-types-by-key id-key]
  (let [prefix (str "update " table " set ")
        body (->>
               (keys entity)
               (filter #(not= nil (% entity)))
               (filter #(not= % id-key))
               (map #(get-sql-update-assignment entity custom-types-by-key %))
               (clojure.string/join ","))
        suffix (str " where " (get-sql-update-assignment entity custom-types-by-key id-key) " and version=" (- (:version entity) 1))
        query (str prefix body suffix)]
    (log/info "[UPDATE-SKIP-NULL]=" query)
    query))