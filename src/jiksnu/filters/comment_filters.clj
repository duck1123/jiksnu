(ns jiksnu.filters.comment-filters
  (:use (ciste [filters :only (deffilter)])
        jiksnu.actions.comment-actions)
  (:require (clj-tigase [core :as tigase]
                        [element :as element])
            (jiksnu [abdera :as abdera])
            (jiksnu.model [activity :as model.activity])))

(deffilter #'add-comment :http
  [action request]
  (-> request :params action))

(deffilter #'fetch-comments :http
  [action request]
  (-> request :params :id show action))

(deffilter #'new-comment :http
  [action request]
  (-> request :params :id model.activity/show))

(deffilter #'comment-response :xmpp
  [action request]
  (if (not= (:to request) (:from request))
    (let [packet (:packet request)
          items (:items request)]
      (action (map #(entry->activity
                     (abdera/parse-xml-string
                      (str (first (element/children %)))))
                   items)))))

(deffilter #'fetch-comments :xmpp
  [action request]
  (let [{{id :id} :params} request]
    (if-let [activity (model.activity/show id)]
      (action activity))))

(deffilter #'fetch-comments-remote :xmpp
  [action request])

