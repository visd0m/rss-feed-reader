(ns rss-feed-reader.jobs.feed-items-deleting-job
  (:require [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.rss.apis :as rss]
            [rss-feed-reader.model.feed-item-model :as feed-item])
  (:import (java.time.temporal ChronoUnit)
           (java.sql Timestamp)
           (java.time Instant)))

(defn is-feed-item-present?
  [feed-item items-to-delete]
  (let [hashes-to-delete (->> items-to-delete
                              (map #(rss/get-feed-item-hash %)))
        item-hash (rss/get-feed-item-hash feed-item)]
    (some #(= item-hash %) hashes-to-delete)))

(defn should-consider-feed?
  [feed items-to-delete]
  (let [feed-items (rss/fetch-feed (get feed :url))]
    (not-any? #(is-feed-item-present? % items-to-delete) feed-items)))

(defn delete-old-feed-items
  []
  (let [date (Timestamp/from (.minus (Instant/now) 3 ChronoUnit/DAYS))
        items-to-delete (feed-item/by-date-before date)]
    (if-not (empty? items-to-delete)
      (let [feeds-to-delete (->> (feed/all-enabled)
                                 (filter #(should-consider-feed? % items-to-delete))
                                 (map #(:id %)))]
        (if-not (empty? feeds-to-delete)
          (feed-item/delete-older-than date feeds-to-delete))))))