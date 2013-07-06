(ns jiksnu.triggers.resource-triggers
  (:use [ciste.initializer :only [definitializer]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.actions.resource-actions :as actions.resource]
            [jiksnu.channels :as ch]
            [jiksnu.model.resource :as model.resource]
            [jiksnu.ops :as ops]
            [lamina.core :as l]))

(defn handle-alternate-link
  [item link]
  (condp = (:type link)
    "application/atom+xml" (let [source (ops/get-source (:href link))]
                             (model.resource/set-field! item :updateSource (:_id source))
                             #_(actions.feed-source/update source))
    nil))

(defn add-link-trigger
  [m]
  (let [[item link] (:args m)]
    (condp = (:rel link)
      "alternate" (handle-alternate-link item link)
      nil)))

(defn handle-pending-get-resource*
  [url]
  (actions.resource/find-or-create {:url url}))

(defn handle-pending-update-resources*
  [url & [options]]
  (let [resource (actions.resource/find-or-create {:url url})]
    (actions.resource/update* (log/spy :info resource) options)))

(def handle-pending-get-resource     (ops/op-handler handle-pending-get-resource*))
(def handle-pending-update-resources (ops/op-handler handle-pending-update-resources*))

(defn init-receivers
  []
  ;; (l/receive-all ch/resource-links-added     add-link-trigger)
  (l/receive-all ch/pending-get-resource     handle-pending-get-resource)
  (l/receive-all ch/pending-update-resources handle-pending-update-resources))

(defonce receivers (init-receivers))
