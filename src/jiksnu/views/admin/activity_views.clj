(ns jiksnu.views.admin.activity-views
  (:use (ciste [views :only [defview]])
        jiksnu.actions.admin.activity-actions))


;; TODO: This page should use a single column
(defview #'index :html
  [request activities]
  {:title "Activities"
   :single true
   :body
   [:table.table
    [:thead
     [:tr
      [:th "user"]
      [:th "title"]]]
    [:tbody
     (map
      (fn [activity]
        [:tr
         [:td (-> activity get-author :username)]
         [:td (:title activity)]])
      activities)]]})
