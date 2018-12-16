(ns rss-feed-reader.model.auto-complete)

(defn autocomplete-id
  "set entity id if missing"
  [subscription]
  (if-not (:id subscription)
    (assoc subscription :id (java.util.UUID/randomUUID))
    subscription))

(defn autocomplete-insert-date
  "set entity insert-date if missing"
  [subscription]
  (if-not (:insert_date subscription)
    (assoc subscription :insert_date (java.sql.Timestamp/from (java.time.Instant/now)))
    subscription))

(defn autocomplete-version
  "set entity version if missing"
  [subscription]
  (if-not (:version subscription)
    (assoc subscription :version 0)
    subscription))