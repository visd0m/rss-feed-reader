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
                         (XmlReader.))
          feed-items (-> (SyndFeedInput.)
                         (.build xml-reader)
                         (bean)
                         (:entries))]

      (for [^SyndEntry feed-item feed-items]
        {:title          (.getTitle feed-item)
         :author         (.getAuthor feed-item)
         :link           (.getLink feed-item)
         :published-date (.getPublishedDate feed-item)}))

    (catch Exception e
      (log/error "an error occurred fetching subscription=" subscription-url ", error=" e))))

(defn get-feed-item-hash
  [feed-item]
  (let [bytes (.getBytes (str (:title feed-item) (:link feed-item)))
        hash (-> (MessageDigest/getInstance "MD5")
                 (.digest bytes))]
    (String. (.encode (Base64/getEncoder) hash) "UTF-8")))

(defn fetch-all-subscriptions
  []
  (let [subscriptions (subscription/all)]
    (doseq [subscription subscriptions
            feed-item (fetch-subscription (:url subscription))]
      (let [hash (get-feed-item-hash feed-item)]
        (when (empty? (feed-item/by-hash hash))
          (feed-item/insert {:subscription-id (:id subscription)
                             :item            (generate-string feed-item)
                             :hash            hash}))))))

