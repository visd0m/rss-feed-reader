(ns rss-feed-reader.app
  (:import (com.rometools.rome.io SyndFeedInput XmlReader)
           (java.net URL))
  (:use ring.adapter.jetty)
  (:require [rss-feed-reader.models.subscription :as subscription]
            [migratus.core :as migratus]
            [rss-feed-reader.models.db :refer :all]
            [bidi.ring :refer (make-handler)]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clojure.tools.logging :as log]
            [cheshire.core :refer :all]))

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
  (let [result (subscription/by-id (:id route-params))]
    (if result
      (res/response result)
      (res/not-found "not found"))))

; ==== post

(defn post-subscription-handler [req]
  (let [subscription (:body req)]
    (log/info subscription)
    (->
      subscription
      (subscription/insert)
      (res/response))))

(def routes ["/subscriptions"
             {""        {:get  (fn [req] (get-list-subscriptions-handler req))
                         :post (fn [req] (post-subscription-handler req))}
              ["/" :id] {:get (fn [req] (get-subscription-handler req))}}])

(def handler (make-handler routes))

(def app
  (-> handler
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-json-response)))

(defn -main
  []
  (migratus/migrate {:store                :database,
                     :migration-dir        "migrations/",
                     :init-in-transaction? false,
                     :db                   db-config})

  (log/info "booting server ... ")
  (run-jetty app {:join? false :port 3000}))