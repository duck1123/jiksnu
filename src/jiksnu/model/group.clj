(ns jiksnu.model.group
  (:use [jiksnu.transforms :only [set-_id set-created-time
                                  set-updated-time]]
        [slingshot.slingshot :only [throw+]]
        [validateur.validation :only [validation-set presence-of]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.query :as mq]
            [monger.result :as result])
  (:import jiksnu.model.Group))

(defonce page-size 20)
(def collection-name "groups")

(def create-validators
  (validation-set
   (presence-of :_id)
   (presence-of :created)
   (presence-of :updated)))

(defn prepare
  [group]
  (-> group
      set-_id
      set-created-time
      set-updated-time))

(defn drop!
  []
  (mc/remove collection-name))

(defn delete
  [group]
  (let [result (mc/remove-by-id collection-name (:_id group))]
    (if (result/ok? result)
      group)))

(defn fetch-by-id
  [id]
  (if-let [group (mc/find-map-by-id collection-name id)]
    (model/map->Group group)))

(defn create
  [group]
  (let [group (prepare group)
        errors (create-validators group)]
    (if (empty? errors)
      (do
        (log/debugf "Creating group: %s" (pr-str group))
        (mc/insert "groups" group)
        (fetch-by-id (:_id group)))
      (throw+ {:type :validation
               :errors errors}))))

(defn fetch-all
  ([] (fetch-all {}))
  ([params] (fetch-all params {}))
  ([params options]
     (let [page (get options :page 1)]
       (let [records  (mq/with-collection collection-name
                        (mq/find params)
                        (mq/paginate :page page :per-page 20))]
         (map model/map->Group records)))))

(defn fetch-by-name
  [name]
  (model/map->Group (mc/find-one-as-map "groups"{:nickname name})))

(defn count-records
  ([] (count-records {}))
  ([params]
     (mc/count collection-name params)))
