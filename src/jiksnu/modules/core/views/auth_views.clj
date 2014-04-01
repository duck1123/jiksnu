(ns jiksnu.modules.core.views.auth-views
  (:require [ciste.views :refer [defview]]
            [ciste.sections.default :refer [show-section]]
            [clojure.tools.logging :as log]
            jiksnu.actions.auth-actions
            [jiksnu.modules.web.sections.auth-sections :as sections.auth]
            [jiksnu.routes.helpers :refer [named-path]]))

;; guest-login

(defview #'guest-login :html
  [request user]
  {:status 303
   :template false
   :session {:pending-id (:_id user)}
   :headers {"Location" (named-path "password page")}})

;; login

(defview #'login :html
  [request user]
  (if user
    {:session {:id (:_id user)}
     :status 303
     :template false
     :headers {"Location" (named-path "public timeline")}}))

(defview #'login :text
  [request user]
  {:session {:id (:_id user)}
   :body (format "logged in as %s" (:username user))})

(defview #'login :json
  [request user]
  {:session {:id (:_id user)}
   :body (format "logged in as %s" (:username user))})

;; login-page

(defview #'login-page :html
  [request _]
  {:title "Login"
   :body
   [:div
    [:div
     [:form {:method "post" :action (named-path "login page")}
      [:fieldset
       [:legend "Login"]
       [:div.clearfix
        [:label {:for "username"} "Username"]
        [:div.input
         [:input {:type "text" :name "username"}]]]
       [:div.clearfix
        [:label {:for "password"} "Password"]
        [:div.input
         [:input {:type "password" :name "password"}]]]
       [:div.actions
        [:input.btn.primary {:type "submit" :value "Login"}]]]]]
    [:div
     [:form {:method "post" :action (named-path "guest login page")}
      [:fieldset
       [:legend "Guest Login"]
       [:div.clearfix
        [:label {:for "webid"} "Web Id"]
        [:div.input
         [:input {:type "text" :name "webid"}]]]
       [:div.actions
        [:input.btn.primary {:type "submit" :value "Login"}]]]]]]})

(defview #'login-page :viewmodel
  [request _]
  {:body {:title "Login"}}
  )

;; logout

(defview #'logout :html
  [request successful]
  (if successful
    {:session {:id nil}
     :status 303
     :template false
     :headers {"Location" (named-path "public timeline")}}))

;; password-page

(defview #'password-page :html
  [request user]
  {:body (sections.auth/password-page user)})

;; show

(defview #'show :model
  [request item]
  {:body (doall (show-section item))})

;; verify-credentials

(defview #'verify-credentials :json
  [request _]
  {:body {:action "error"
          :message "Could not authenticate you"
          :request (:uri request)}
   :template false})

;; whoami

(defview #'whoami :text
  [request user]
  {:body user})

(defview #'whoami :json
  [request user]
  {:body user})
