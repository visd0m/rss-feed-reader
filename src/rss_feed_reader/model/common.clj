(ns rss-feed-reader.model.common
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.model.auto-complete :as auto-complete]))

; == auto complete

(defn autocomplete-insert
  "auto complete missing field of subscription"
  [entity]
  (-> entity
      (auto-complete/autocomplete-id)
      (auto-complete/autocomplete-insert-date)
      (auto-complete/autocomplete-version)))

(defn autocomplete-update
  "auto complete missing field of subscription"
  [entity]
  (-> entity
      (auto-complete/autocomplete-update-date)
      (auto-complete/autocomplete-version)))

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
                         (str "('" (% entity) "'" "::" type ")")
                         (str "'" (% entity) "'")))
                 (clojure.string/join ","))
        query (str "insert into " table " (" entity-keys ") values (" values ")")]
    (log/info "[INSERT]=" query)
    query))

; === update

(defn- get-sql-update-assignment
  [entity custom-types-by-key key]
  (str (clojure.string/replace (clojure.core/name key) "-" "_") "=" (if-let [type (key custom-types-by-key)]
                                                                      (str "('" (key entity) "'" "::" type ")")
                                                                      (str "'" (key entity) "'"))))

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



