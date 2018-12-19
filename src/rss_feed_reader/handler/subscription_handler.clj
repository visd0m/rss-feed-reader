(ns rss-feed-reader.handler.subscription-handler
  (:require [rss-feed-reader.model.subscription-model :as subscription]
            [clojure.tools.logging :as log]
            [ring.util.response :as res]))

; === GET

(defn get-list-subscriptions
  [req]
  (log/info req)
  (res/response (subscription/all)))

(defn get-subscription
  [{:keys [params]}]
  (log/info params)
  (if-let [result (subscription/by-id (:id params))]
    (res/response result)
    (res/not-found "not found")))

; === POST

(defn post-subscription
  "doc"
  [req]
  (let [subscription (:body req)]
    (log/info "trying inserting subscription=" subscription)
    (if (empty? (subscription/by-url (:url subscription)))
      (-> subscription
          (subscription/insert)
          (res/response))
      (res/bad-request (str "url=" (:url subscription) " already present")))))