(ns rss-feed-reader.pushnews.terminal-notifier-job
  (:require [rss-feed-reader.model.feed-item-model :as feed_items]
            [rss-feed-reader.model.subscription-model :as subscriptions]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.model.subscription-model :as subscription])
  (:import (java.sql Timestamp)
           (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn notify
  [title message link]
  (shell/sh
    "terminal-notifier"
    "-title" title
    "-message" message
    "-open" link))

(defn push-news
  []
  (let [news (feed_items/by-date-after (Timestamp/from (.minus (Instant/now) 1 (ChronoUnit/MINUTES))))]
    (log/info "news to notify=" news)
    (let [feed-ids (->> news
                        (map :feed-id)
                        (distinct))
          subscriptions-by-id (apply array-map
                                     (mapcat (fn [x] [(:id x) x])
                                             (->> (subscription/batch-by-feed-id feed-ids)
                                                  (filter :enabled))))
          consumers-to-notify (->> (vals subscriptions-by-id)
                                   (map :consumer-id)
                                   (distinct))])
    (let [news+subscription-tag (->> news
                                     (map (fn [to-notify]
                                            (assoc to-notify :tag (:tag (subscriptions/by-id (:subscription_id to-notify)))))))]
      (doseq [to-notify news+subscription-tag]
        (notify
          (:tag to-notify)
          (get-in to-notify [:item "title"])
          (get-in to-notify [:item "link"]))))))