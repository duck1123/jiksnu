(ns jiksnu.modules.admin.views.subscription-views
  (:use [ciste.sections.default :only [show-section]]
        [ciste.views :only [defview]]
        [jiksnu.modules.admin.actions.subscription-actions :only [index delete show]]
        [jiksnu.modules.core.sections :only [admin-index-section admin-show-section
                                             format-page-info
                                            ]]
        [jiksnu.modules.web.sections :only [pagination-links with-page]])
  (:require [taoensso.timbre :as log])
  (:import jiksnu.model.Subscription))

;; (defview #'admin-index :html
;;   [request subscriptions]
;;   {:body (sections.subscription/index-section subscriptions)})

;; delete

(defview #'delete :html
  [request _]
  {:status 303
   :flash "subscription deleted"
   :template false
   :headers {"Location" "/admin/subscriptions"}})

;; index

(defview #'index :html
  [request {:keys [items] :as page}]
  {:title "Subscriptions"
   :status 200
   :single true
   :body (with-page "subscriptions"
           (pagination-links page)
           (admin-index-section items page))})

;; show

(defview #'show :html
  [request subscription]
  {:title "Subscription"
   :body (admin-show-section subscription)})

(defview #'show :model
  [request subscription]
  {:body (show-section subscription)})
