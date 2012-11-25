(ns jiksnu.model.feed-source
  (:use [jiksnu.transforms :only [set-_id set-created-time set-updated-time]]
        [slingshot.slingshot :only [throw+]]
        [validateur.validation :only [acceptance-of validation-set presence-of]])
  (:require [clj-statsd :as s]
            [clj-time.core :as time]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [lamina.trace :as trace]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.query :as mq])
  (:import jiksnu.model.FeedSource))

(def collection-name "feed_sources")

(def create-validators
  (validation-set
   (presence-of :_id)
   (presence-of :topic)
   (presence-of :domain)
   (acceptance-of :local         :accept (partial instance? Boolean))
   (presence-of :status)
   (presence-of :created)
   (presence-of :updated)))

(defn set-field!
  "Updates user's field to value"
  [user field value]
  (log/debugf "setting %s (%s = %s)" (:_id user) field value)
  (mc/update collection-name
             {:_id (:_id user)}
             {:$set {field value}}))

(defn fetch-by-id
  [id]
  (when-let [record (mc/find-map-by-id collection-name id)]
    (model/map->FeedSource record)))

(defn update
  [source params]
  (mc/update collection-name
             (select-keys source [:_id])
             params)
  (fetch-by-id (:_id source)))

(defn push-value!
  [source key value]
  (update source
    {:$addToSet {key value}}))

(defn create
  [params]
  (let [errors (create-validators params)]
    (if (empty? errors)
      (do
        (log/debugf "Creating feed source: %s" params)
        (mc/insert collection-name params)
        ;; TODO: check no errors
        (let [item (fetch-by-id (:_id params))]
          (trace/trace :feed-sources:created item)
          (s/increment "feed-sources_created")
          item))
      (throw+ {:type :validation :errors errors}))))

(def delete        (model/make-deleter collection-name))
(def drop!         (model/make-dropper collection-name))
(def count-records (model/make-counter collection-name))

(defn find-record
  [options & args]
  (if-let [record (mc/find-one-as-map collection-name options)]
    (model/map->FeedSource record)))

(defn fetch-all
  ([] (fetch-all {}))
  ([params] (fetch-all params {}))
  ([params options]
     ((model/make-fetch-fn model/map->FeedSource collection-name)
      params options)))

(defn fetch-by-topic
  "Fetch a single source by it's topic id"
  [topic]
  (find-record {:topic topic}))

(defn find-by-user
  "Returns the source associated with a user"
  [user]
  (fetch-by-id (:update-source user)))

(defn ensure-indexes
  []
  (doto collection-name
   (mc/ensure-index {:topic 1} {:unique true})))
