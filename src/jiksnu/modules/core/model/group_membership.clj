(ns jiksnu.modules.core.model.group-membership
  (:require [jiksnu.modules.core.model :as model]
            [jiksnu.modules.core.templates.model :as templates.model]
            [jiksnu.modules.core.validators :as vc]
            [jiksnu.transforms :refer [set-_id set-created-time
                                       set-updated-time]]
            [validateur.validation :as v])
  (:import (org.bson.types ObjectId)
           (org.joda.time DateTime)))

(def collection-name "group_memberships")
(def maker           #'model/map->GroupMembership)
(def page-size       20)

(def create-validators
  (v/validation-set
   (vc/type-of :_id     ObjectId)
   (vc/type-of :created DateTime)
   (vc/type-of :updated DateTime)))

(def count-records (templates.model/make-counter       collection-name))
(def delete        (templates.model/make-deleter       collection-name))
(def drop!         (templates.model/make-dropper       collection-name))
(def remove-field! (templates.model/make-remove-field! collection-name))
(def set-field!    (templates.model/make-set-field!    collection-name))
(def fetch-by-id   (templates.model/make-fetch-by-id   collection-name maker))
(def create        (templates.model/make-create        collection-name #'fetch-by-id #'create-validators))
(def fetch-all     (templates.model/make-fetch-fn      collection-name maker))
