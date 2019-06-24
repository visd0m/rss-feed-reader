(ns rss-feed-reader.jobs.rss-fetching-job
  (:require [cheshire.core :refer :all]
            [clojure.core.async :refer [go]]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.jobs.common :as jobs-common]
            [rss-feed-reader.rss.apis :as rss]
            [clojure.tools.logging :as log]))

(def job-lock-name
  "FETCH_FEED_LOCKED")

(defn- perform-operation
  []
  (let [feeds (feed/all-enabled)]
    (doseq [feed feeds
            feed-item (rss/safe-fetch-feed (:url feed))]
      (let [hash (rss/get-feed-item-hash feed-item)]
        (when-not (feed-item/by-hash hash)
          (feed-item/insert {:feed-id (:id feed)
                             :item    (generate-string feed-item)
                             :hash    hash}))))))

(defn fetch-all-feeds
  []
  (try
    (jobs-common/with-lock perform-operation job-lock-name)
    (catch Exception e
      (log/error (str "error fetching feeds=" e)))))