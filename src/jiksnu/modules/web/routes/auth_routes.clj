(ns jiksnu.modules.web.routes.auth-routes
  (:require [cemerick.friend :as friend]
            [jiksnu.modules.core.actions.auth-actions :as actions.auth]
            [jiksnu.modules.core.actions.user-actions :as actions.user]
            [jiksnu.modules.http.resources :refer [add-group! defresource defgroup]]
            [jiksnu.modules.web.core :refer [jiksnu]]
            [jiksnu.modules.web.helpers :refer [angular-resource page-resource]]
            [liberator.representation :refer [as-response ring-response]]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as timbre]))

(defn login-malformed?
  [{{{:keys [username password]} :params} :request}]
  (when (or username password)
    (if (and username password)
      [false {:username username :password password}]
      true)))

(defgroup jiksnu auth
  :name "Authentication"
  :description "Authentication routes")

(defresource auth :register
  :url "/main/register"
  :allowed-methods [:get :post]
  :methods {:get {:summary "Register Page"
                  :state "registerPage"}
            :post {:summary "Do Register"
                   :parameters {:username {:in :formData
                                           :type "string"}}
                   :response {"409" {:description "Username conflict"}}}}
  :mixins [angular-resource]
  :parameters {}
  :available-media-types ["text/html"]
  :post! (fn [ctx]
           (let [params (:params (:request ctx))
                 data (try+
                       (when-let [{:keys [username]} (actions.user/register params)]
                         (ring-response
                          (friend/merge-authentication
                           {:body "ok"}
                           {:username username :identity username})))
                       (catch [:type :conflict] ex
                         (ring-response ex {:status 409}))
                       (catch [:type :missing-param] ex
                         (ring-response ex {:status 400})))]
             {:data data}))
  :handle-created :data)

(defresource auth :remote
  :url "/main/remote"
  :mixins [angular-resource]
  :methods {:get  {:summary "Login Page"
                   :state "loginPage"}}

  )

(defresource auth :login
  :url "/main/login"
  :mixins [angular-resource]
  :allowed-methods [:get :post]
  :methods {:get  {:summary "Login Page"
                   :state "loginPage"}
            :post {:summary "Do Login"
                   :parameters {:username {:in :formData
                                           :type "string"
                                           :description "The username"
                                           :required true}
                                :password {:in :formData
                                           :type "string"
                                           :description "the password"
                                           :required true}}
                   :responses {"200" {:description "Login Response"}}}}
  :available-media-types ["text/html" "application/json"]
  :malformed? login-malformed?
  :authorized? (fn [{:keys [username password]}]
                 (if (and username password)
                   (if-let [auth (:username (actions.auth/login username password))]
                     {:authorized-username auth})
                   true))
  :post-redirect? false
  :new? false
  :respond-with-entity? false
  :handle-no-content (fn [ctx]
                       (ring-response
                        (friend/merge-authentication
                         {}
                         {:identity (:authorized-username ctx)}))))

(defresource auth :logout
  :url                   "/main/logout"
  :allowed-methods       [:post]
  :available-media-types ["application/json"]
  :methods {:post {:summary "Do Logout"}}
  :post! (fn [ctx]
           (timbre/info "logout handler")
           true)
  :handle-created (fn [ctx]
                    (ring-response
                     (friend/logout* (as-response {:data "ok"} ctx)))))

(defresource auth :verify
  :name "Verify Credentials"
  :methods {:get {:summary "Verify Credentials"}
            :post {:summary "Verify Credentials"}}
  :url "/api/account/verify_credentials.json"
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :post! (fn [ctx] true)
  :exists? (fn [ctx]
             {:data (actions.auth/verify-credentials)}))
