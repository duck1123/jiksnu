(ns jiksnu.transforms.conversation-transforms
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.route-one.core :as r]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.model :as model]
            [jiksnu.routes.helpers :as rh])
  (:import java.net.URI))

(defn set-local
  [conversation]
  (if (contains? conversation :local)
    conversation
    (assoc conversation :local
           (let [url (URI. (:url conversation))]
             (= (:_id (actions.domain/current-domain))
                (.getHost url))))))

(defn set-update-source
  [conversation]
  (if (:update-source conversation)
    conversation
    (if-let [url (:url conversation)]
      (let [resource (model/get-resource url)]
        (if-let [source (if (:local resource)
                       (model/get-source (rh/formatted-url "show conversation" {:id (:_id conversation)} "atom"))
                       (try
                         (actions.feed-source/discover-source url)
                         (catch RuntimeException ex)))]
          (assoc conversation :update-source (:_id source))
          (throw+ "could not determine source")))
      (throw+ "Could not determine url"))))

(defn set-url
  [item]
  (if (:url item)
    item
    (when (:local item)
      (assoc item :url (r/named-url "show conversation" {:id (:_id item)})))))

(defn set-domain
  [item]
  (if (:domain item)
    item
    (if-let [domain (if (:local item)
                      (actions.domain/current-domain)
                      (when-let [uri (URI. (:url item))]
                        (when-let [domain-name (.getHost uri)]
                          (actions.domain/find-or-create {:_id domain-name}))))]
      (assoc item :domain (:_id domain))
      (throw+ "Could not determine domain"))))
