(ns rss-feed-reader.app
  (:use ring.adapter.jetty)
  (:require [rss-feed-reader.handler.feed-item-handler :as feed-item-handler]
            [rss-feed-reader.handler.subscription-handler :as subscription-handler]
            [rss-feed-reader.handler.feed-handler :as feed-handler]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [bidi.ring :refer (make-handler)]
            [clojure.tools.logging :as log]
            [migratus.core :as migratus]
            [rss-feed-reader.config.db :refer :all]
            [rss-feed-reader.config.jobs :refer :all :as jobs])
  (:gen-class))

; ==== APIs

(def routes ["/feed"
             [["" {:get  #(feed-handler/get-all %)
                   :post #(feed-handler/post %)}]]
             "/subscriptions"
             [["" {:get  #(subscription-handler/get-all %)
                   :post #(subscription-handler/post %)}]
              [["/" :id]
               [["" {:get    #(subscription-handler/get %)
                     :delete #(subscription-handler/delete %)}]
                ["/feed_items" {:get #(feed-item-handler/get-list-feed-items %)}]]]]])

; ====

(def handler
  (make-handler routes))

(def app
  (-> handler
      (wrap-params)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-json-response)))

(def migratus-config {:store                :database,
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