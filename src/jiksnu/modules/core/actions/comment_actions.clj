(ns jiksnu.modules.core.actions.comment-actions
  (:require [jiksnu.modules.core.actions.activity-actions :as actions.activity]
            [jiksnu.modules.core.model.activity :as model.activity]
            [jiksnu.namespace :as ns]))

;; TODO: What id should be used here?
(defn comment-node-uri
  [{id :id}]
  (str ns/microblog ":replies:item=" id))

(defn add-comment
  [params]
  (if-let [parent (model.activity/fetch-by-id (:id params))]
    (actions.activity/post
     (-> params
         (assoc :parent (:_id parent))
         (assoc-in [:object :type] "comment")))))

(defn comment-response
  [activities]
  (actions.activity/remote-create activities))

;; TODO: fetch all in 1 request
(defn fetch-comments
  [activity]
  (let [comments (map model.activity/fetch-by-id
                      (:comments activity))]
    [activity comments]))
