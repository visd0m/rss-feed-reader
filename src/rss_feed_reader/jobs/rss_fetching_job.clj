(ns rss-feed-reader.jobs.rss-fetching-job
  (:require [cheshire.core :refer :all]
            [clojure.core.async :refer [go]]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.rss.apis :as rss]))

(defn fetch-all-feeds
  []
  (let [feeds (feed/all-enabled)]
    (doseq [feed feeds
            feed-item (rss/fetch-feed-or-disable feed)]
      (let [hash (rss/get-feed-item-hash feed-item)]
        (when (empty? (feed-item/by-hash hash))
          (feed-item/insert {:feed-id (:id feed)
                             :item    (generate-string feed-item)
                             :hash    hash}))))))
