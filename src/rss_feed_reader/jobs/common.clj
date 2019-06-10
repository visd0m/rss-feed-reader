(ns rss-feed-reader.jobs.common
  (:require [rss-feed-reader.model.configuration-model :as configuration]
            [clojure.tools.logging :as log]))

(defn acquire-lock
  [lock-name]
  (if-let [lock (configuration/get-key lock-name)]
    (if-not (Boolean/parseBoolean (:value lock))
      (configuration/put-key {:key lock-name :value true})
      (log/warn (str "job with lock=" lock-name " already locked")))
    (throw (RuntimeException. (str "can not acquire lock=" lock-name)))))

(defn release-lock
  [lock-name]
  (if-let [_ (configuration/get-key lock-name)]
    (configuration/put-key {:key lock-name :value false})
    (throw (RuntimeException. (str "can not release lock=" lock-name)))))

(defn with-lock
  [what-to-do lock-name]
  (try
    (do
      (acquire-lock lock-name)
      (what-to-do))
    (catch Exception e
      (log/error (str "an error occurred during with-lock execution, error=" e)))
    (finally
      (release-lock lock-name))))