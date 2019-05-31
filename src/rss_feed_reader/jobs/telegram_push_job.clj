(ns rss-feed-reader.jobs.telegram-push-job
  (:require [rss-feed-reader.model.consumer-model :as consumer]
            [rss-feed-reader.model.subscription-model :as subscription]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.telegram.apis :as telegram]
            [clojure.tools.logging :as log])
  (:import (java.time.temporal ChronoUnit)
           (java.time Instant)
           (java.sql Timestamp)))

(defn- push-news-consumer
  [consumer]
  (let [subscriptions (subscription/by-consumer-id (:id consumer))
        enabled-subscriptions (->> subscriptions
                                   (filter :enabled))]
    (when-not (empty? enabled-subscriptions)
      (let [subscriptions-by-feed-id (apply array-map (->> enabled-subscriptions
                                                           (mapcat (fn [subscription]
                                                                     [(:feed_id subscription) subscription]))))
            feeds (feed/batch-by-id (->> enabled-subscriptions (map :feed_id)))
            enabled-feeds (->> feeds (filter :enabled))]
        (when-not (empty? enabled-feeds)
          (let [feed-items (feed-item/batch-by-feed-id-and-date-after
                             (->> enabled-feeds (map :id))
                             (Timestamp/from (.minus (Instant/now) 30 (ChronoUnit/SECONDS))))]
            (doseq [feed-item feed-items]
              (let [subscription (get subscriptions-by-feed-id (:feed_id feed-item))]
                (telegram/send-message {:text    (str (get subscription :tag) "\n\n"
                                                      (get-in feed-item [:item "title"]) "\n\n"
                                                      (get-in feed-item [:item "author"]) "\n\n"
                                                      (get-in feed-item [:item "link"]))
                                        :chat-id (:external_id consumer)})))))))))

(defn push-news
  []
  (let [consumers (consumer/all-enabled)]
    (doseq [consumer consumers]
      (try
        (push-news-consumer consumer)
        (catch Exception error
          (log/error "error pushing news to consumer=" consumer " error=" error))))))
