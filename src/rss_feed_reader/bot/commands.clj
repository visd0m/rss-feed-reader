(ns rss-feed-reader.bot.commands
  (:require [rss-feed-reader.telegram.apis :as telegram]
            [rss-feed-reader.model.feed-model :as feed]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.model.subscription-model :as subscription]
            [rss-feed-reader.model.consumer-model :as consumer]
            [rss-feed-reader.rss.apis :as rss]
            [clojure.tools.logging :as log])
  (:import (java.time.temporal ChronoUnit)
           (java.time Instant)
           (java.sql Timestamp)))

; === commands

(def welcome-message
  "Welcome to visd0m feeder")

(def welcome-back-message
  "Welcome back to visd0m feeder")

(def help-message
  (str "/start\n"
       "start using visd0m feeder bot\n\n"
       "/stop\n"
       "stop using visd0m feeder bot\n\n"
       "/subscribe <url> <tag>\n"
       "subscribe to rss feed <url> tagged with <tag> tag\n\n"
       "/bytag <tag>\n"
       "retrieve last news by feed identified by tag <tag>\n\n"
       "/recent\n"
       "retrieve last news by all yours subscriptions\n\n"
       "/list\n"
       "list all your subscriptions\n\n"
       "/disable <tag>\n"
       "disable subscription identified by tag <tag>\n"))

(defmulti handle-command
          (fn [command]
            (log/info "handling command=" (get-in command [:message :text]))
            (first (:parsed-command command))))

; === help

(defmethod handle-command "/help" [command]
  (telegram/send-message {:text    help-message
                          :chat-id (get-in command [:message :chat :id])}))

; ==== start

(defn- start-new-consumer
  [chat-id command]
  (do
    (consumer/insert {:external-id chat-id
                      :name        (get-in command [:message :from :first_name])
                      :surname     (get-in command [:message :from :last_name])
                      :username    (get-in command [:message :from :username])
                      :enabled     true})
    (telegram/send-message {:text    welcome-message
                            :chat-id (get-in command [:message :chat :id])})))

(defn- start-existing-consumer
  [consumer command]
  (if-not (:enabled consumer)
    (do
      (consumer/update-skip-null {:id      (:id consumer)
                                  :version (:version consumer)
                                  :enabled true})
      (telegram/send-message {:text    welcome-back-message
                              :chat-id (get-in command [:message :chat :id])}))
    (telegram/send-message {:text    "already subscribed to the best feed bot ever"
                            :chat-id (get-in command [:message :chat :id])})))

(defmethod handle-command "/start" [command]
  (let [chat-id (get-in command [:message :chat :id])
        consumer (consumer/by-external-id chat-id)]

    (if consumer
      (start-existing-consumer consumer command)
      (start-new-consumer chat-id command))))

; ==== list

(defmethod handle-command "/list" [command]
  (let [consumer (consumer/by-external-id (get-in command [:message :chat :id]))
        subscriptions-by-feed-id (apply array-map (->> (subscription/by-consumer-id (:id consumer))
                                                       (filter :enabled)
                                                       (mapcat (fn [subscription]
                                                                 [(:feed_id subscription) subscription]))))]
    (if-not (empty? subscriptions-by-feed-id)
      (let [feeds (feed/batch-by-id (keys subscriptions-by-feed-id))]
        (doseq [feed feeds]
          (let [subscription (get subscriptions-by-feed-id (:id feed))]
            (telegram/send-message {:text    (str (:tag subscription) "\n"
                                                  (:url feed))
                                    :chat-id (get-in command [:message :chat :id])}))))
      (telegram/send-message {:text    "Wow such emptiness"
                              :chat-id (get-in command [:message :chat :id])}))))

; ==== stop

(defmethod handle-command "/stop" [command]
  (if-let [consumer (consumer/by-external-id (get-in command [:message :chat :id]))]
    (do
      (consumer/update-skip-null {:id      (:id consumer)
                                  :version (:version consumer)
                                  :enabled false})
      (telegram/send-message {:text    "Farewell"
                              :chat-id (get-in command [:message :chat :id])}))
    (telegram/send-message {:text    "I don't even know you"
                            :chat-id (get-in command [:message :chat :id])})))

; ==== recent

(defmethod handle-command "/recent" [command]
  (let [consumer (consumer/by-external-id-enabled (get-in command [:message :chat :id]))]
    (if consumer
      (let [subscriptions-by-feed-id (apply array-map (->> (subscription/by-consumer-id (:id consumer))
                                                           (filter :enabled)
                                                           (mapcat (fn [subscription]
                                                                     [(:feed_id subscription) subscription]))))
            feeds (feed/batch-by-id (->> (vals subscriptions-by-feed-id)
                                         (filter :enabled)
                                         (map :feed_id)))
            feed-items (feed-item/batch-by-feed-id-and-date-after (->> feeds (map :id))
                                                                  (Timestamp/from (.minus (Instant/now) 1 (ChronoUnit/HOURS))))]
        (if-not (empty? feed-items)
          (doseq [feed-item feed-items]
            (let [subscription (get subscriptions-by-feed-id (:feed_id feed-item))]
              (telegram/send-message {:text    (str (get subscription :tag) "\n\n"
                                                    (get-in feed-item [:item "title"]) "\n\n"
                                                    (get-in feed-item [:item "author"]) "\n\n"
                                                    (get-in feed-item [:item "link"]))
                                      :chat-id (get-in command [:message :chat :id])})))
          (telegram/send-message {:text    "Wow such emptiness"
                                  :chat-id (get-in command [:message :chat :id])})))
      (telegram/send-message {:text    "Disabled consumer, enable yourself through /start"
                              :chat-id (get-in command [:message :chat :id])}))))

; ==== subscribe

(defn- is-valid-rss-feed
  [url]
  (try
    (rss/fetch-feed url)
    true
    (catch Exception _
      false)))

(defn- on-existing-feed
  [consumer tag feed chat-id]
  ; enable again feed if it was disabled
  (when-not (:enabled feed)
    (feed/update-skip-null {:id      (:id feed)
                            :version (:version feed)
                            :enabled true}))
  (if-let [subscription (first (subscription/by-feed-id-and-consumer-id (:id feed) (:id consumer)))]
    ; on existing subscription re-enable and re-tag it
    (subscription/update-skip-null {:id      (:id subscription)
                                    :version (:version subscription)
                                    :tag     tag
                                    :enabled true})
    (subscription/insert {:tag         tag
                          :feed-id     (:id feed)
                          :consumer-id (:id consumer)
                          :enabled     true}))

  (telegram/send-message {:text    (str "subscribed to= " (:url feed)
                                        " as= " tag)
                          :chat-id chat-id}))

(defn- on-missing-feed
  [consumer url tag chat-id]
  (if (and consumer url tag (is-valid-rss-feed url))
    (let [feed (feed/insert {:url url})]
      (do
        (subscription/insert {:tag         tag
                              :feed-id     (:id feed)
                              :consumer-id (:id consumer)})
        (telegram/send-message {:text    (str "subscribed to= " url
                                              " as= " tag)
                                :chat-id chat-id})))
    (telegram/send-message {:text    (str "invalid rss feed url=" url)
                            :chat-id chat-id})))

(defmethod handle-command "/subscribe" [command]
  (if-not (= (count (:parsed-command command)) 3)
    (telegram/send-message {:text    (str "invalid syntax for 'subscribe' command\n"
                                          "syntax: /subscribe <url> <tag>")
                            :chat-id (get-in command [:message :chat :id])})
    (let [chat-id (get-in command [:message :chat :id])
          consumer (consumer/by-external-id chat-id)
          url (nth (:parsed-command command) 1)
          tag (nth (:parsed-command command) 2)
          feed (feed/by-url url)]

      (when (and consumer url tag)
        (if feed
          (on-existing-feed consumer tag feed chat-id)
          (on-missing-feed consumer url tag chat-id))))))

; === disable

(defmethod handle-command "/disable" [command]
  (let [chat-id (get-in command [:message :chat :id])]
    (if-not (= (count (:parsed-command command)) 2)
      (telegram/send-message {:text    (str "invalid syntax for 'disable' command\n"
                                            "syntax: /disable <tag>")
                              :chat-id chat-id})
      (let [tag (nth (:parsed-command command) 1)
            subscription (first (subscription/by-tag tag))]
        (if subscription
          (do
            (subscription/update-skip-null {:id      (:id subscription)
                                            :version (:version subscription)
                                            :enabled false})
            (telegram/send-message {:text    (str "you won't receive anymore news about " tag)
                                    :chat-id chat-id}))
          (telegram/send-message {:text    (str "no subscription found for tag=" tag)
                                  :chat-id chat-id}))))))

; === bytag

(defmethod handle-command "/bytag" [command]
  (if-not (= (count (:parsed-command command)) 2)
    (telegram/send-message {:text    (str "invalid syntax for 'bytag' command\n"
                                          "syntax: /bytag <tag>")
                            :chat-id (get-in command [:message :chat :id])})
    (let [tag (nth (:parsed-command command) 1)
          subscription (first (subscription/by-tag tag))]
      (if (and tag subscription)
        (let [feed (feed/by-id (:feed_id subscription))
              feed-items (feed-item/by-feed-id-and-date-after (:id feed) (Timestamp/from (.minus (Instant/now) 1 (ChronoUnit/HOURS))))]
          (if-not (empty? feed-items)
            (doseq [feed-item feed-items]
              (telegram/send-message {:text    (str (get subscription :tag) "\n\n"
                                                    (get-in feed-item [:item "title"]) "\n\n"
                                                    (get-in feed-item [:item "author"]) "\n\n"
                                                    (get-in feed-item [:item "link"]))
                                      :chat-id (get-in command [:message :chat :id])}))
            (telegram/send-message {:text    "Wow such emptiness"
                                    :chat-id (get-in command [:message :chat :id])})))
        (telegram/send-message {:text    (str "no subscription found for tag=" tag)
                                :chat-id (get-in command [:message :chat :id])})))))

; === default

(defmethod handle-command :default [command]
  (telegram/send-message {:text    (str (get-in command [:message :text]) "? dunno what to do")
                          :chat-id (get-in command [:message :chat :id])}))
