(ns rss-feed-reader.telegram.telegram-fetcher
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [rss-feed-reader.model.configuration-model :as configuration]
            [rss-feed-reader.model.consumer-model :as consumer]
            [rss-feed-reader.model.subscription-model :as subscription]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.rss.rss-fetcher :as rss]
            [clojure.tools.logging :as log])
  (:import (java.sql Timestamp)
           (java.time Instant)
           (java.time.temporal ChronoUnit)))

; ==== telegram apis

(def bot-token
  (slurp "resources/token.txt"))

(def base-url
  (str "https://api.telegram.org/bot" bot-token "/"))

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

(defn send-message
  [message]
  (client/get (str base-url "sendMessage")
              {:query-params {:chat_id (:chat-id message)
                              :text    (:text message)}}))

; commands

(def welcome-message
  "Welcome to visd0m feeder")

(def welcome-back-message
  "Welcome back to visd0m feeder")

(defmulti handle-command
          (fn [command]
            (log/info "handling command=" (get-in command [:message :text]))
            (first (:parsed-command command))))

; ==== start

(defn- start-new-consumer
  [chat-id command]
  (do
    (consumer/insert {:external-id chat-id
                      :name        (get-in command [:message :from :first_name])
                      :surname     (get-in command [:message :from :last_name])
                      :username    (get-in command [:message :from :username])
                      :enabled     true})
    (send-message {:text    welcome-message
                   :chat-id (get-in command [:message :chat :id])})))

(defn- start-existing-consumer
  [consumer command]
  (if-not (:enabled consumer)
    (do
      (consumer/update-skip-null {:id      (:id consumer)
                                  :version (:version consumer)
                                  :enabled true})
      (send-message {:text    welcome-back-message
                     :chat-id (get-in command [:message :chat :id])}))))

(defmethod handle-command "/start" [command]
  (let [chat-id (get-in command [:message :chat :id])
        consumer (consumer/by-external-id chat-id)]

    (if consumer
      (start-existing-consumer consumer command)
      (start-new-consumer chat-id command))

    (log/info "handled command start")))

; ==== stop

(defmethod handle-command "/stop" [command]
  (if-let [consumer (consumer/by-external-id (get-in command [:message :chat :id]))]
    (do
      (consumer/update-skip-null {:id      (:id consumer)
                                  :version (:version consumer)
                                  :enabled false})
      (send-message {:text    "Farewell"
                     :chat-id (get-in command [:message :chat :id])})))

  (log/info "handled command stop"))

; ==== recent

(defmethod handle-command "/recent" [command]
  (let [consumer (consumer/by-external-id (get-in command [:message :chat :id]))
        subscriptions-by-feed-id (apply array-map (->> (subscription/by-consumer-id (:id consumer))
                                                       (filter :enabled)
                                                       (mapcat (fn [subscription]
                                                                 [(:feed_id subscription) subscription]))))
        feeds (feed/batch-by-id (->> (vals subscriptions-by-feed-id)
                                     (map :feed_id)))
        feed-items (feed-item/batch-by-feed-id-and-date-after (->> feeds (map :id))
                                                              (Timestamp/from (.minus (Instant/now) 30 (ChronoUnit/MINUTES))))]
    (doseq [feed-item feed-items]
      (let [subscription (get subscriptions-by-feed-id (:feed_id feed-item))]
        (send-message {:text    (str (get subscription :tag) "\n\n"
                                     (get-in feed-item [:item "title"]) "\n\n"
                                     (get-in feed-item [:item "author"]) "\n\n"
                                     (get-in feed-item [:item "published-date"]) "\n\n"
                                     (get-in feed-item [:item "link"]))
                       :chat-id (get-in command [:message :chat :id])})))

    (log/info "handled command recent")))

; ==== subscribe

(defn is-valid-rss-feed
  [url]
  (try
    (rss/fetch-feed url)
    true
    (catch Exception _
      false)))

(defn on-existing-feed
  [consumer tag feed]
  ; enable again feed if it was disabled
  (when-not (:enabled feed)
    (feed/update-skip-null {:id      (:id feed)
                            :version (:version feed)
                            :enabled true}))
  (if-let [subscription (first (subscription/by-feed-id-and-consumer-id (:id feed) (:id consumer)))]
    ; on existing subscription re-enable it if disabled
    (when-not (:enabled subscription)
      (subscription/update-skip-null {:id      (:id subscription)
                                      :version (:version subscription)
                                      :enabled true}))
    (subscription/insert {:tag         tag
                          :feed-id     (:id feed)
                          :consumer-id (:id consumer)})))

(defn on-missing-feed
  [consumer url tag]
  (when (and consumer url tag (is-valid-rss-feed url))
    (let [feed (feed/insert {:url url})]
      (subscription/insert {:tag         tag
                            :feed-id     (:id feed)
                            :consumer-id (:id consumer)}))))

(defmethod handle-command "/subscribe" [command]
  (let [chat-id (get-in command [:message :chat :id])
        consumer (consumer/by-external-id chat-id)
        url (nth (:parsed-command command) 1)
        tag (nth (:parsed-command command) 2)
        feed (first (feed/by-url url))]

    (when (and consumer url tag)
      (if feed
        (on-existing-feed consumer tag feed)
        (on-missing-feed consumer url tag))))

  (log/info "handled command subscribe"))

; === by-tag

(defmethod handle-command "/bytag" [command]
  (if-not (= (count (:parsed-command command)) 2)
    (send-message {:text    (str "invalid syntax for 'bytag' command\n"
                                 "syntax: /bytag <tag>")
                   :chat-id (get-in command [:message :chat :id])})
    (let [tag (nth (:parsed-command command) 1)
          subscription (first (subscription/by-tag tag))]
      (if (and tag subscription)
        (let [feed (first (feed/by-id (:feed_id subscription)))
              feed-items (feed-item/by-feed-id-and-date-after (:id feed) (Timestamp/from (.minus (Instant/now) 30 (ChronoUnit/MINUTES))))]
          (doseq [feed-item feed-items]
            (send-message {:text    (str (get subscription :tag) "\n\n"
                                         (get-in feed-item [:item "title"]) "\n\n"
                                         (get-in feed-item [:item "author"]) "\n\n"
                                         (get-in feed-item [:item "published-date"]) "\n\n"
                                         (get-in feed-item [:item "link"]))
                           :chat-id (get-in command [:message :chat :id])})))
        (send-message {:text    (str "no subscription found for tag=" tag)
                       :chat-id (get-in command [:message :chat :id])})))))

(defmethod handle-command :default [command]
  (let [response (send-message {:text    "unhandled command"
                                :chat-id (get-in command [:message :chat :id])})]
    (log/info "send-message response=" response)))

; ===

(def last-update-id-configuration-key
  "last-update-id")

(defn fetch-commands
  []
  (let [last-update-id-configuration (configuration/get-key last-update-id-configuration-key)
        messages (:result (get-updates last-update-id-configuration))]

    ; example of telegram message
    ;
    ;{:update_id 913163047,
    ; :message {:message_id 16990,
    ;           :from {:id 40978885,
    ;                  :is_bot false,
    ;                  :first_name "Domenico",
    ;                  :last_name "Visconti",
    ;                  :username "visd0m",
    ;                  :language_code "it"},
    ;           :chat {:id 40978885,
    ;                  :first_name "Domenico",
    ;                  :last_name "Visconti",
    ;                  :username "visd0m",
    ;                  :type "private"},
    ;           :date 1558108817,
    ;           :text "fewef"}}

    (let [commands (->> messages
                        (filter (fn [message]
                                  (log/info message)
                                  (clojure.string/starts-with? (get-in message [:message :text]) "/"))))]
      (log/info "commands received=" commands)
      (doseq [command commands]
        (handle-command
          (assoc
            command
            :parsed-command
            (clojure.string/split (get-in command [:message :text]) #" ")))))

    (configuration/put-key {:key   last-update-id-configuration-key
                            :value (:update_id (last messages))})))