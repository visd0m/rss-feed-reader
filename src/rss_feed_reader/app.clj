(ns rss-feed-reader.app
  (:import (com.rometools.rome.io SyndFeedInput XmlReader)
           (java.net URL))
  (:use ring.adapter.jetty)
  (:require [rss-feed-reader.models.subscription :as subscription])
  (:require [rss-feed-reader.models.db :refer :all]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [bidi.ring :refer (make-handler)]
            [clojure.tools.logging :as log]
            [migratus.core :as migratus]

            [rss-feed-reader.scheduler.jobs :refer :all :as jobs]))

(defn get-feed
  "get feed from subscription url"
  [url]
  (let [xmlReader (new XmlReader (new URL url))]
    (let [feed (.build (new SyndFeedInput) xmlReader)] feed)))

; ==== GET

(defn- get-list-subscriptions-handler
  [req]
  (log/info req)
  (res/response (subscription/all)))

(defn- get-subscription-handler
  [{:keys [route-params]}]
  (log/info route-params)
  (let [result (subscription/by-id (:id route-params))]
    (if result
      (res/response result)
      (res/not-found "not found"))))

; ==== POST

(defn- post-subscription-handler
  "doc"
  [req]
  (let [subscription (:body req)]
    (log/info "trying inserting subscription=" subscription)
    (if (empty? (subscription/by-url (:url subscription)))
      (-> subscription
          (subscription/insert)
          (res/response))
      (res/bad-request (str "url=" (:url subscription) " already present")))))

; ==== APIs

(def routes ["/subscriptions"
             {""        {:get  (fn [req] (get-list-subscriptions-handler req))
                         :post (fn [req] (post-subscription-handler req))}
              ["/" :id] {:get (fn [req] (get-subscription-handler req))}}])

(def handler
  (make-handler routes))

; ====

(def app
  (-> handler
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-json-response)))

(defn -main
  ([]
   (-main true))
  ([join]
   (log/info "starting migrations ... ")
   (migratus/migrate {:store                :database,
                      :migration-dir        "migrations/",
                      :init-in-transaction? false,
                      :db                   db-config})

   (log/info "booting scheduler ... ")
   (jobs/start-scheduler-with-jobs)

   (log/info "booting scheduler ... ")
   (run-jetty app {:join? join :port 3000})))