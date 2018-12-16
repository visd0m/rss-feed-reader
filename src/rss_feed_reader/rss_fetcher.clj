(ns rss-feed-reader.rss-fetcher
  (:import (com.rometools.rome.io SyndFeedInput XmlReader)
           (java.net URL)
           (java.security MessageDigest)
           (java.util Base64)
           (java.lang String)
           (com.rometools.rome.feed.synd SyndEntry))
  (:require [clojure.tools.logging :as log]
            [cheshire.core :refer :all]
            [rss-feed-reader.model.subscription :as subscription]
            [rss-feed-reader.model.feed-item :as feed-item]))

(defn fetch-subscription
  [subscription-url]
  (log/info "fetching subscription=" subscription-url)

  (try
    (let [xml-reader (-> subscription-url
                         (URL.)
                         (XmlReader.))]

      (let [feed-items (-> (SyndFeedInput.)
                           (.build xml-reader)
                           (bean)
                           (:entries))]

        (map (fn [^SyndEntry feed-item]
               {:title          (.getTitle feed-item)
                :author         (.getAuthor feed-item)
                :link           (.getLink feed-item)
                :published-date (.getPublishedDate feed-item)}) feed-items)))

    (catch Exception e
      (log/error "an error occurred fetching subscription=" subscription-url ", error=" e))))

(defn get-feed-item-hash
  [feed-item]
  (let [bytes (-> (MessageDigest/getInstance "MD5")
                  (.digest (.getBytes (str (:title feed-item) (:link feed-item)))))]
    (String. (.encode (Base64/getEncoder) bytes) "UTF-8")))

(defn fetch-all-subscriptions
  []
  (let [subscriptions (subscription/all)]
    (doseq [subscription subscriptions]
      (doseq [feed-item (fetch-subscription (:url subscription))]
        (let [hash (get-feed-item-hash feed-item)]
          (if (empty? (feed-item/by-hash hash))
            (feed-item/insert {:subscription-id (:id subscription)
                               :item            (generate-string feed-item)
                               :hash            hash})))))))
