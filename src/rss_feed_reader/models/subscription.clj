(ns rss-feed-reader.models.subscription
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as sql]
            [clojure.spec.alpha :as s]
            [rss-feed-reader.models.db :as db]
            [rss-feed-reader.models.auto-complete :as auto-complete]))


(s/def ::insert-subscription (s/keys :req-un [::url]
                                     :opt-un [::id ::insert_date ::version ::update_date]))

; ==== load

(defn all
  []
  "get list of all subscriptions"
  (log/info "getting all subscriptions")
  (into [] (sql/query db/pg-db ["select * from subscription order by insert_date desc"])))

(defn by-id
  [id]
  "get subscription by id"
  (log/info "loading subscription for id=" id)
  (sql/query db/pg-db ["select * from subscription where id = (?::uuid)" id]))

; ==== insert

(defn autocomplete-subscription-insert
  "auto complete missing field of subscription"
  [subscription]
  (-> subscription
      (auto-complete/autocomplete-id)
      (auto-complete/autocomplete-insert-date)
      (auto-complete/autocomplete-version)))

(defn insert
  "insert given subscription if valid"
  [subscription]
  {:pre [(s/valid? ::subscription subscription)]}

  (let [autocompleted-subscription (autocomplete-subscription-insert subscription)]
    (log/info "creating subscription=" autocompleted-subscription)
    (sql/execute! db/pg-db [
                            "insert into subscription values(
                            (?::uuid),
                            ?,
                            (?::TIMESTAMP),
                            (?::TIMESTAMP),
                            ?)"
                            (:id autocompleted-subscription)
                            (:url autocompleted-subscription)
                            (:insert_date autocompleted-subscription)
                            (:update_date autocompleted-subscription)
                            (:version autocompleted-subscription)])))
