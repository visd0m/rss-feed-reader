(ns rss-feed-reader.model.auto-complete)

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

(defn autocomplete-version
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