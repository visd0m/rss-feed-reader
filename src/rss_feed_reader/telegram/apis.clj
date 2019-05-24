(ns rss-feed-reader.telegram.apis
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]))

(def bot-token
  (slurp "/Users/domenicovisconti/Dev/Clojure/rss_feed_reader/resources/token.txt"))

(def base-url
  (str "https://api.telegram.org/bot" bot-token "/"))

; === getUpdates

(defn get-updates
  [last-update-id]
  (let [query-params {:allowed_updates "message"
                      :timeout         8}]
    (-> (client/get (str base-url "getUpdates")
                    {:query-params (if last-update-id
                                     (assoc query-params :offset (+ (Integer/parseInt (:value last-update-id)) 1))
                                     query-params)})
        (:body)
        (parse-string true))))

; === sendMessage

(defn send-message
  [message]
  (client/get (str base-url "sendMessage")
              {:query-params {:chat_id (:chat-id message)
                              :text    (:text message)}}))
