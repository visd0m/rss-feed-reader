(ns rss-feed-reader.handler.subscription-handler
  (:require [rss-feed-reader.model.subscription-model :as subscription]
            [clojure.tools.logging :as log]
            [ring.util.response :as res]
            [clojure.spec.alpha :as s]))

(s/def ::post-subscription (s/keys :req-un [::url]))

; === GET

(defn get-all
  "load list of all subscriptions without paginating"
  [req]
  (log/info req)
  (res/response (subscription/all)))

(defn get
  "load subscription by id"
  [{:keys [params]}]
  (log/info params)
  (if-let [result (subscription/by-id (:id params))]
    (res/response result)
    (res/not-found "not found")))

; === POST

(defn post
  "create new subscription"
  [{subscription :body}]
  {:pre [(s/valid? ::post-subscription subscription)]}
  (log/info "trying inserting subscription=" subscription)
  (if (empty? (subscription/by-url (:url subscription)))
    (-> subscription
        (subscription/insert)
        (res/response))
    (res/bad-request (str "url=" (:url subscription) " already present"))))

(defn delete
  "logically delete subscription"
  [{:keys [params]}]
  (log/info "deleting subscription")
  (if-let [subscription (subscription/by-id (:id params))]
    (subscription/update-skip-null {:id      (:id subscription)
                                    :version (:version subscription)
                                    :enabled false})
    (res/bad-request (str "can not find subscription for id=" (:id params)))))