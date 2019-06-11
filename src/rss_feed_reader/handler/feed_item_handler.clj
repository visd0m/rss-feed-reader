(ns rss-feed-reader.handler.feed-item-handler
  (:require [ring.util.response :as res]
            [rss-feed-reader.handler.common :refer :all]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.model.feed-model :as feed]
            [clojure.tools.logging :as log]))

(defn get-list-feed-items
  "get paginated list of feed items"
  [{path-params  :params
    query-params :query-params}]
  (log/info path-params)
  (log/info query-params)

  (if-let [feed (feed/by-id (:id path-params))]
    (let [starting-after (get-starting-after query-params #(:order_unique (feed-item/by-id %)))
          limit (get-limit query-params)
          search query-params
          items (-> (:id feed)
                    (feed-item/by-feed-id starting-after search (+ 1 limit)))]
      (res/response (get-paginated-list items limit)))
    (res/not-found "not found")))
