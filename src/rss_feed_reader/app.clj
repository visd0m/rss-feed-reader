(ns rss-feed-reader.app
  (:use ring.adapter.jetty)
  (:require [rss-feed-reader.model.subscription :as subscription])
  (:require [rss-feed-reader.model.db :refer :all]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [bidi.ring :refer (make-handler)]
            [clojure.tools.logging :as log]
            [migratus.core :as migratus]
            [rss-feed-reader.scheduler.jobs :refer :all :as jobs]))

; ==== GET

(defn- get-list-subscriptions-handler
  [req]
  (log/info req)
  (res/response (subscription/all)))

(defn- get-subscription-handler
  [{:keys [route-params]}]
  (log/info route-params)
  (if-let [result (subscription/by-id (:id route-params))]
    (res/response result)
    (res/not-found "not found")))

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

; ====

(def handler
  (make-handler routes))

(def app
  (-> handler
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-json-response)))

(def migratus-config
  {:store                :database,
   :migration-dir        "migrations/",
   :init-in-transaction? false,
   :db                   db-config})

; ==== main

(defn -main
  ([]
   (-main true))
  ([join]
   (log/info "starting migrations ... ")
   (migratus/migrate migratus-config)

   (log/info "starting scheduler ... ")
   (jobs/start-scheduler-with-jobs)

   (log/info "booting server ... ")
   (run-jetty app {:join? join :port 3000})))