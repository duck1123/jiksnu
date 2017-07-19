(ns jiksnu.modules.core.actions.dialback-actions
  (:require [jiksnu.modules.core.model.dialback :as model.dialback]
            [jiksnu.modules.core.templates.actions :as templates.actions]
            [jiksnu.transforms :as transforms]))

(def index*    (templates.actions/make-indexer 'jiksnu.modules.core.model.dialback :sort-clause {:date 1}))

(defn index
  [& options]
  (apply index* options))

(defn prepare-create
  [activity]
  (transforms/set-_id activity))

(defn create
  [params]
  (let [item (model.dialback/create params)]
    (model.dialback/fetch-by-id (:_id item))))

;; (defn delete
;;   [activity]
;;   (let [actor-id (session/current-user-id)
;;         author (:author activity)]
;;     (if (or (session/is-admin?) (= actor-id author))
;;       (model.activity/delete activity)
;;       ;; TODO: better exception type
;;       (throw+ {:type :authorization
;;                :msg "You are not authorized to delete that activity"}))))

(defn confirm
  [params]
  ;; TODO: validate
  true)
