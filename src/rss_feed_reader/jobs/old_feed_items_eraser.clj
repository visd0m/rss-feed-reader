(ns rss-feed-reader.jobs.old-feed-items-eraser
  (:require [rss-feed-reader.model.feed-item-model :as feed-item])
  (:import (java.time.temporal ChronoUnit)))

(defn delete-old-feed-items
  []
  (feed-item/delete-older-than
    (java.sql.Timestamp/from (.minus (java.time.Instant/now) 3 ChronoUnit/DAYS))))