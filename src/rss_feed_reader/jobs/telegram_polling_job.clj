(ns rss-feed-reader.jobs.telegram-polling-job
  (:require [cheshire.core :refer :all]
            [rss-feed-reader.model.configuration-model :as configuration]
            [clojure.core.async :refer [go]]
            [clojure.tools.logging :as log]
            [rss-feed-reader.telegram.apis :as telegram-apis]
            [rss-feed-reader.bot.commands :as bot-commands]))

(def last-update-id-configuration-key
  "last-update-id")

(defn handle-command-async
  [command]
  (go
    (let [parsed-command (assoc
                           command
                           :parsed-command
                           (clojure.string/split (get-in command [:message :text]) #" "))]
      (bot-commands/handle-command parsed-command))))

(defn fetch-commands
  []
  (let [last-update-id-configuration (configuration/get-key last-update-id-configuration-key)
        messages (:result (telegram-apis/get-updates last-update-id-configuration))]

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
                        (filter #(get-in % [:message :text]))
                        (filter (fn [message]
                                  (log/info message)
                                  (clojure.string/starts-with? (get-in message [:message :text]) "/"))))]
      (log/info "commands received=" commands)

      (configuration/put-key {:key   last-update-id-configuration-key
                              :value (:update_id (last messages))})

      (doseq [command commands]
        (try
          (handle-command-async command)
          (catch Exception error
            (log/warn "error processing command=" command " , error=" error)))))))
