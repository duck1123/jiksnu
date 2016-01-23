(ns jiksnu.modules.web.views.stream-views
  (:require [ciste.views :refer [defview]]
            [ciste.sections.default :refer [index-section show-section]]
            [jiksnu.actions.stream-actions :as actions.stream]
            [jiksnu.modules.web.sections :refer [bind-to redirect with-sub-page]]
            [jiksnu.modules.web.sections.activity-sections :as sections.activity])
  (:import jiksnu.model.Conversation))

(defview #'actions.stream/create :html
  [request item]
  (redirect "/"
            "user has been created"))

(defview #'actions.stream/group-timeline :html
  [request [group {:keys [items] :as page}]]
  {:title (str (:nickname group) " group")
   :post-form true
   :body
   (bind-to "targetGroup"
            (show-section group)
            [:div {:data-model "group"}
             (index-section items)])})

(defview #'actions.stream/home-timeline :html
  [request activities]
  {:title "Home Timeline"
   :post-form true
   :body (index-section activities)})

(defview #'actions.stream/public-timeline :html
  [request page]
  (let [items [(Conversation.)]]
    {:title "Public Timeline"
     :post-form true
     :formats (sections.activity/index-formats items)
     :body
     [:div {:ui-view ""}
      "View goes here"]}))

(defview #'actions.stream/user-timeline :html
  [request [user {:keys [items] :as page}]]
  {:user user
   :title (:name user)
   :post-form true
   :body
   (bind-to "targetUser"
            [:div {:data-model "user"}
             (with-sub-page "activities"
               (index-section items page))])
   :formats (sections.activity/timeline-formats user)})
