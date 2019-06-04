(ns rss-feed-reader.telegram.apis
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log]))

(def bot-token
  (System/getenv "BOT_TOKEN"))

(def base-url
  (str "https://api.telegram.org/bot" bot-token "/"))

; === getUpdates

(defn get-updates
  [last-update-id timeout]
  (let [query-params {:allowed_updates "message"
                      :timeout         timeout}]
    (let [result (-> (client/get (str base-url "getUpdates")
                                 {:query-params (if last-update-id
                                                  (assoc query-params :offset (+ (Long/parseLong (:value last-update-id)) 1))
                                                  query-params)})
                     (:body)
                     (parse-string true))]
      (log/info result)
      result)))

; === sendMessage

(defn send-message
  [message]
  (client/get (str base-url "sendMessage")
              {:query-params {:chat_id (:chat-id message)
                              :text    (:text message)}}))
