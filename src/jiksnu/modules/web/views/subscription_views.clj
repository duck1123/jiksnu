(ns jiksnu.modules.web.views.subscription-views
  (:use [ciste.views :only [defview]]
        [ciste.sections.default :only [uri]]
        jiksnu.actions.subscription-actions
        [jiksnu.modules.web.sections :only [bind-to with-sub-page
                                            redirect pagination-links]])
  (:require [taoensso.timbre :as log]
            [jiksnu.model.subscription :as model.subscription]
            [jiksnu.modules.core.sections.subscription-sections :as sections.subscription])
  (:import jiksnu.model.Subscription))

(defview #'delete :html
  [request _]
  (redirect "/admin/subscriptions"))

(defview #'get-subscribers :html
  [request [user {:keys [items] :as page}]]
  {:title "Subscribers"
   :body
   (bind-to "targetUser"
            [:div {:data-model "user"}
             (with-sub-page "subscribers"
               (pagination-links page)
               (sections.subscription/subscribers-section items page))])})

(defview #'get-subscriptions :html
  [request [user {:keys [items] :as page}]]
  {:title "Subscriptions"
   :body
   (bind-to "targetUser"
            [:div {:data-model "user"}
             (with-sub-page "subscriptions"
               (pagination-links page)
               (sections.subscription/subscriptions-section items page))])})

(defview #'ostatussub-submit :html
  [request subscription]
  (redirect "/" "The request has been sent"))

(defview #'subscribe :html
  [request subscription]
  (redirect "/"))

(defview #'unsubscribe :html
  [request subscription]
  (redirect "/"))
