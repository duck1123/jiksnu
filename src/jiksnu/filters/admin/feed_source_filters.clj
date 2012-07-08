(ns jiksnu.filters.admin.feed-source-filters
  (:use [ciste.filters :only [deffilter]]
        jiksnu.actions.admin.feed-source-actions
        [jiksnu.filters :only [parse-page parse-sorting]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.model :as model]
            [jiksnu.model.feed-source :as model.feed-source]))

(deffilter #'index :http
  [action request]
  (action
   {} (merge
       {}
       (parse-page request)
       (parse-sorting request))))

(deffilter #'show :http
  [action request]
  (if-let [source (-> request :params :id model/make-id model.feed-source/fetch-by-id)]
    (action source)))

(deffilter #'delete :http
  [action request]
  (if-let [source (-> request :params :id model/make-id model.feed-source/fetch-by-id)]
    (action source)))

;; (deffilter #'add-source :http
;;   [action request]
;;   (if-let [source (-?> request :params :id model/make-id model.feed-source/fetch-by-id)]
;;     (if-let [watcher (-?> request :params :user_id)])
;;     )
;;   )
