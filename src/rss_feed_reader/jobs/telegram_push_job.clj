(ns rss-feed-reader.jobs.telegram-push-job
  (:require [rss-feed-reader.model.consumer-model :as consumer]
            [rss-feed-reader.model.subscription-model :as subscription]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.telegram.apis :as telegram]
            [clojure.tools.logging :as log]
            [rss-feed-reader.jobs.common :as jobs-common])
  (:import (java.time Instant)
           (java.sql Timestamp)))

(def job-lock-name
  "TELEGRAM_PUSH_LOCKED")

(defn push-news-consumer
  [consumer]
  (let [subscriptions (->> (subscription/by-consumer-id (:id consumer))
                           (filter :enabled))]

    (doseq [subscription subscriptions]
      (let [feed (feed/by-id (:feed-id subscription))
            last-check-date (:last-check-date subscription)]
        (when (:enabled feed)
          (let [feed-items (feed-item/by-feed-id-and-date-after (:id feed) (if (nil? last-check-date) (Timestamp/from (Instant/now)) last-check-date))]

            (doseq [feed-item feed-items]
              (telegram/send-message {:text    (str (get subscription :tag) "\n\n"
                                                    (get-in feed-item [:item "title"]) "\n\n"
                                                    (get-in feed-item [:item "author"]) "\n\n"
                                                    (get-in feed-item [:item "link"]))
                                      :chat-id (:external-id consumer)})))))

      (subscription/update-skip-null {:id              (:id subscription)
                                      :version         (:version subscription)
                                      :last-check-date (Timestamp/from (Instant/now))}))))

(defn perform-operation
  []
  (let [consumers (consumer/all-enabled)]
    (doseq [consumer consumers]
      (try
        (push-news-consumer consumer)
        (catch Exception error
          (log/error "error pushing news to consumer=" consumer " error=" error))))))

(defn push-news
  []
  (try
    (jobs-common/with-lock perform-operation job-lock-name)
    (catch Exception e
      (log/error (str "error fetching feeds=" e)))))
