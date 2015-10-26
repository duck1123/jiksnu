(ns jiksnu.modules.http
  (:require [active.timbre-logstash :refer [timbre-json-appender]]
            [ciste.loader :refer [defmodule]]
            jiksnu.modules.core.formats
            jiksnu.modules.core.views
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :refer [println-appender spit-appender]]
            [taoensso.timbre.profiling :as profiling]
            [socket-rocket.logstash :as sr]))

(defn start
  []
  (log/info "starting http")


  (log/info "before set")
  (log/merge-config!
   {:appenders {
                :logstash (timbre-json-appender "192.168.1.151" 4660)
                ;; :logstash sr/logstash-appender
                ;; :println (println-appender {:stream :auto})
                ;; :spit (spit-appender)
                }
    :shared-appender-config {:logstash {:port 4660 :logstash "192.168.1.151"}}})
  (log/info "after set"))

(defmodule "http"
  :start start
  :deps ["jiksnu.modules.core"])
