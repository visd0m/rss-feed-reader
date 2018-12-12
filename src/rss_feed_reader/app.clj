(ns rss-feed-reader.app
  (:import (com.rometools.rome.io SyndFeedInput XmlReader)
           (java.net URL))
  (:use rss-feed-reader.migrations
        ring.adapter.jetty)
  (:require [rss-feed-reader.models.subscription :as subscription]
            [migratus.core :as migratus]
            [bidi.ring :refer (make-handler)]
            [ring.middleware.flash :refer [wrap-flash]]
            [rss-feed-reader.subscription]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response]]
            [clojure.tools.logging :as log]))

(defn get-feed
  "get feed from subscription url"
  [url]
  (let [xmlReader (new XmlReader (new URL url))]
    (let [feed (.build (new SyndFeedInput) xmlReader)] feed)))

; ==== get

(defn get-list-subscriptions-handler
  [req]
  (log/info req)
  (res/response (subscription/all)))

(defn get-subscription-handler
  [{:keys [route-params]}]
  (log/info route-params)
  (res/response (subscription/by-id (:id route-params))))

; ==== post

(defn post-subscription-handler [req]
  (res/response (subscription/insert (:body req))))



(def routes ["/subscriptions"
             {""        {:get  (fn [req] (get-list-subscriptions-handler req))
                         :post (fn [req] (post-subscription-handler req))}
              ["/" :id] {:get (fn [req] (get-subscription-handler req))}}])

(def handler (make-handler routes))

(def app
  (-> handler
      wrap-json-response))

(defn -main
  []
  (migratus/migrate config)
  (run-jetty app {:port 3000}))