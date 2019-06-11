(ns rss-feed-reader.handler.consumer-handler
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.model.consumer-model :as consumer]
            [ring.util.response :as res]))

; === GET

(defn get-all
  [req]
  (log/info req)
  (res/response (consumer/all-enabled)))

(defn get
  "Load consumer by id"
  [{:keys [params]}]
  (log/info params)
  (if-let [result (consumer/by-id (:id params))]
    (res/response result)
    (res/not-found "not found")))

; === DELETE

(defn delete
  [{:keys [params]}]
  (log/info params)
  (if-let [consumer (consumer/by-id (:id params))]
    (res/response (consumer/update-skip-null {:id      (:id consumer)
                                              :version (:version consumer)
                                              :enabled false}))
    (res/not-found "not found")))
