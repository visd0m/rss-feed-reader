(ns rss-feed-reader.handler.subscription-handler
  (:require [rss-feed-reader.model.subscription-model :as subscription]
            [rss-feed-reader.model.feed-model :as feed]
            [clojure.tools.logging :as log]
            [ring.util.response :as res]
            [clojure.spec.alpha :as s]))

(s/def ::post-subscription (s/keys :req-un [::feed-id ::tag ::consumer-id]))

; === GET

(defn get-all
  "Load list of all subscriptions without pagination"
  [req]
  (log/info req)
  (res/response (subscription/all-enabled)))

(defn get
  "Load subscription by id"
  [{:keys [params]}]
  (log/info params)
  (if-let [result (subscription/by-id (:id params))]
    (res/response result)
    (res/not-found "not found")))

; === POST

(defn post
  "Create new subscription"
  [{subscription :body}]
  {:pre [(s/valid? ::post-subscription subscription)]}
  (log/info "trying inserting subscription=" subscription)
  (let [feed (feed/by-id (:feed-id subscription))]
    (if feed
      (-> subscription
          (subscription/insert)
          (res/response))
      (res/bad-request (str "can not find feed for id=" (:feed-id subscription))))))

(defn delete
  "Logically delete subscription"
  [{:keys [params]}]
  (log/info "deleting subscription")
  (if-let [subscription (subscription/by-id (:id params))]
    (subscription/update-skip-null {:id      (:id subscription)
                                    :version (:version subscription)
                                    :enabled false})
    (res/bad-request (str "can not find subscription for id=" (:id params)))))