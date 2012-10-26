(ns jiksnu.filters.resource-filters
  (:use [ciste.filters :only [deffilter]]
        jiksnu.actions.resource-actions
        [jiksnu.filters :only [parse-page parse-sorting]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [jiksnu.session :as session]))

;; create

(deffilter #'create :http
  [action request]
  (let [{:keys [params]} request]
    (action params)))

;; delete

(deffilter #'delete :command
  [action id]
  (when-let [item (model.resource/fetch-by-id (model/make-id id))]
    (action item)))

(deffilter #'delete :http
  [action request]
  (let [id (:id (:params request))]
    (when-let [item (model.resource/fetch-by-id (model/make-id id))]
      (action item))))

;; index

(deffilter #'index :http
  [action request]
  (action {} (merge {}
                    (parse-page request)
                    (parse-sorting request))))

;; show

(deffilter #'show :http
  [action request]
  (let [{{id :id} :params} request]
    (if-let [item (model.user/fetch-by-id (model/make-id id))]
     (action item))))

