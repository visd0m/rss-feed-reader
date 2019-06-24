(ns rss-feed-reader.rss.apis
  (:import (com.rometools.rome.io SyndFeedInput XmlReader)
           (java.net URL)
           (java.security MessageDigest)
           (java.util Base64)
           (java.lang String)
           (com.rometools.rome.feed.synd SyndEntry))
  (:require [clojure.tools.logging :as log]
            [cheshire.core :refer :all]
            [clojure.core.async :refer [go]]
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

(defn safe-fetch-feed
  [feed-url]
  (try
    (fetch-feed feed-url)
    (catch Exception e
      (log/error (str "error fetching feed url=" feed-url " error=" e)))))

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
