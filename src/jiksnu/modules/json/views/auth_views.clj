(ns jiksnu.modules.json.views.auth-views
  (:require [ciste.views :refer [defview]]
            [jiksnu.actions.auth-actions :as actions.auth]))

(defview #'actions.auth/login :json
  [request user]
  {:session {:id (:_id user)}
   :body (format "logged in as %s" (:username user))})

(defview #'actions.auth/verify-credentials :json
  [request _]
  {:body {:action "error"
          :message "Could not authenticate you"
          :request (:uri request)}
   :template false})
