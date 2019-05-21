(ns rss-feed-reader.handler.feed-handler
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as res]
            [rss-feed-reader.model.feed-model :as feed]
            [clojure.spec.alpha :as s]))

(s/def ::post-feed (s/keys :req-un [::url]))

; === GET

(defn get-all
  [req]
  (log/info req)
  (res/response (feed/all-enabled)))

(defn get
  "Load feed by id"
  [{:keys [params]}]
  (log/info params)
  (if-let [result (feed/by-id (:id params))]
    (res/response result)
    (res/not-found "not found")))

; === POST

(defn post
  "Create new feed"
  [{feed :body}]
  {:pre [(s/valid? ::post-feed feed)]}
  (log/info "trying inserting feed=" feed)
  (if (empty? (feed/by-url (:url feed)))
    (-> feed
        (feed/insert)
        (res/response))
    (res/bad-request (str "url=" (:url feed) " already present"))))