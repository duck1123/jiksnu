(ns jiksnu.views.admin.group-views
  (:use [ciste.sections.default :only [index-section]]
        [ciste.views :only [defview]]
        [jiksnu.actions.admin.group-actions :only [index]]
        [jiksnu.sections :only [admin-index-section]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.sections.group-sections :as sections.like]))

(defview #'index :html
  [request {:keys [items] :as response}]
  {:single true
   :title "Groups"
   :body (admin-index-section items response)})

(defview #'index :viewmodel
  [request {:keys [items] :as page}]
  {:body {:groups (admin-index-section items page)}})
