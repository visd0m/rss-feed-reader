(ns rss-feed-reader.rss.rss-fetcher
  (:import (com.rometools.rome.io SyndFeedInput XmlReader)
           (java.net URL)
           (java.security MessageDigest)
           (java.util Base64)
           (java.lang String)
           (com.rometools.rome.feed.synd SyndEntry))
  (:require [clojure.tools.logging :as log]
            [cheshire.core :refer :all]
            [rss-feed-reader.model.feed-item-model :as feed-item]
            [rss-feed-reader.model.feed-model :as feed]))

(defn fetch-feed
  [feed-url]
  (log/info "fetching feed=" feed-url)

  (let [xml-reader (-> feed-url
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
       :published-date (.getPublishedDate feed-item)})))

(defn fetch-feed-or-disable
  [feed]
  (try
    (fetch-feed (:url feed))
    (catch Exception e
      (log/error "an error occurred fetching feed=" (:url feed) ", error=" e)
      (feed/update-skip-null {:id      (:id feed)
                              :version (:version feed)
                              :enabled false}))))

(defn get-feed-item-hash
  [feed-item]
  (let [bytes (.getBytes (str (:title feed-item) (:link feed-item)))
        hash (-> (MessageDigest/getInstance "MD5")
                 (.digest bytes))]
    (String. (.encode (Base64/getEncoder) hash) "UTF-8")))

(defn fetch-all-feeds
  []
  (let [feeds (feed/all-enabled)]
    (doseq [feed feeds
            feed-item (fetch-feed-or-disable feed)]
      (let [hash (get-feed-item-hash feed-item)]
        (when (empty? (feed-item/by-hash hash))
          (feed-item/insert {:feed-id (:id feed)
                             :item    (generate-string feed-item)
                             :hash    hash}))))))

