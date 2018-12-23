(ns rss-feed-reader.handler.subscription-handler
  (:require [rss-feed-reader.model.subscription-model :as subscription]
            [clojure.tools.logging :as log]
            [ring.util.response :as res]
            [clojure.spec.alpha :as s]))

(s/def ::post-subscription (s/keys :req-un [::url]))

; === GET

(defn get-list-subscriptions
  "load list of all subscriptions without paginating"
  [req]
  (log/info req)
  (res/response (subscription/all)))

(defn get-subscription
  "load subscription by id"
  [{:keys [params]}]
  (log/info params)
  (if-let [result (subscription/by-id (:id params))]
    (res/response result)
    (res/not-found "not found")))

; === POST

(defn post-subscription
  "create new subscription"
  [{subscription :body}]
  {:pre [(s/valid? ::post-subscription subscription)]}
  (log/info "trying inserting subscription=" subscription)
  (if (empty? (subscription/by-url (:url subscription)))
    (-> subscription
        (subscription/insert)
        (res/response))
    (res/bad-request (str "url=" (:url subscription) " already present"))))