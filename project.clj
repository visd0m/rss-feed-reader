(defproject rss_feed_reader "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.9.0"]

                 [com.rometools/rome "1.12.0"]

                 [org.clojure/java.jdbc "0.7.8"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]

                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-defaults "0.1.4"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-json "0.5.0-beta1"]
                 [bidi "2.1.4"]

                 [migratus "1.2.0"]])

