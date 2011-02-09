(ns jiksnu.http.view.user-view
  (:use [clj-gravatar.core :only (gravatar-image)]
        jiksnu.http.controller.user-controller
        jiksnu.http.view
        jiksnu.model
        jiksnu.session
        jiksnu.view
        ciste.core
        ciste.view)
  (:require [hiccup.form-helpers :as f]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.subscription :as model.subscription]
            [jiksnu.model.user :as model.user])
  (:import jiksnu.model.Activity
           jiksnu.model.User))

(defmethod uri User [user] (str "/" (:_id user)))
(defmethod title User [user] (or (:display-name user)
                                 (:first-name user)
                                 (:_id user)))

(defn avatar-img
  [user]
  (let [{:keys [avatar-url title email domain name]} user]
    [:a.url {:href (uri user)
             :title title}
     [:img.avatar.photo
      {:width "48"
       :height "48"
       :alt name
       :src (or avatar-url
                (gravatar-image email)
                (gravatar-image (str (:_id user)
                                     "@"
                                     domain)))}]
     [:span.nickname.fn
      name]]))

(defmethod show-section-minimal User
  [user & options]
  (avatar-img user))

(defmethod index-line User
  [user]
  [:tr
   [:td (avatar-img user)]
   [:td (:_id user)]
   [:td (:domain user)]
   [:td [:a {:href (uri user)} "Show"]]
   [:td [:a {:href (str (uri user) "/edit")} "Edit"]]
   [:td (f/form-to [:delete (uri user)]
                   (f/submit-button "Delete"))]])

(defmethod add-form User
  [record & options]
  [:div
   [:h3 "Create User"]
   (f/form-to
    [:post "/users"]
    (f/text-field :username)
    (f/submit-button "Add User"))])

(defmethod edit-form User
  [user & options]
  (let [{:keys [domain first-name last-name password
                confirm-password avatar-url]} user]
    [:div
    (f/form-to
     [:post (uri user)]
     [:p (:_id user)]
     [:p (f/label :domain "Domain")
      (f/text-field :domain domain)]
     [:p (f/label :name "Display Name:")
      (f/text-field :name (:name user))]
     [:p (f/label :first-name "First Name:")
      (f/text-field :first-name first-name)]
     [:p (f/label :last-name "Last Name:")
      (f/text-field :last-name last-name)]
     [:p (f/label :password "Password")
      (f/text-field :password password)]
     [:p (f/label :confirm-password "Confirm Password")
      (f/text-field :confirm-password confirm-password)]
     [:p (f/label :admin "Admin?")
      (f/check-box :admin (:admin user))]
     [:p (f/label :debug "Debug?")
      (f/check-box :debug (:debug user))]
     [:p (f/label :avatar-url "Avatar Url:")
      (f/text-field :avatar-url avatar-url)]
     [:p (f/submit-button "Submit")])
    (dump user)]))

(defn subscribe-form
  [user]
  (f/form-to [:post "/main/subscribe"]
             (f/hidden-field :subscribeto (:_id user))
             (f/submit-button "Subscribe")))

(defn unsubscribe-form
  [user]
  (f/form-to [:post "/main/unsubscribe"]
             (f/hidden-field :unsubscribeto (:_id user))
             (f/submit-button "Unsubscribe")))

(defn user-actions
  [user]
  (let [actor-id (current-user-id)]
    (if (= (:_id user) actor-id)
      [:p "This is you!"]
      [:ul
       [:li
        (if (model.subscription/subscribing? actor-id (:_id user))
          (unsubscribe-form user)
          (subscribe-form user))]])))

(defmethod show-section User
  [user]
  (let [actor (current-user-id)]
    (list
     (add-form (Activity.))
     [:div
      ;; [:p "Id:" (:_id user)]
      [:p (avatar-img user)]
      [:p (:first-name user) " " (:last-name user)]
      (if (model.subscription/subscribed? actor (:_id user))
        [:p "This user follows you."])
      (if (model.subscription/subscribing? actor (:_id user))
        [:p "You follow this user."])
      (user-actions user)
      [:div
       [:p "Subscriptions"]
       [:ul
        (map
         (fn [subscription]
           [:li (show-section-minimal
                 (jiksnu.model.user/show (:to subscription)))])
         (model.subscription/subscriptions user))]]
      [:div
       [:p "Subscribers"]
       [:ul
        (map
         (fn [subscriber]
           [:li (show-section-minimal
                 (jiksnu.model.user/show (:from subscriber)))])
         (model.subscription/subscribers user))]]
      [:div.activities
       (map show-section-minimal
            (model.activity/find-by-user user))]
      (dump user)])))

(defmethod index-section User
  [users]
  [:div
   (add-form (User.))
   [:table
    [:thead
     [:tr
      [:th]
      [:th "User"]
      [:th "Domain"]]]
    [:tbody
     (map index-line users)]]])

(defview #'index :html
  [request users]
  {:body (index-section users)})

(defview #'create :html
  [request user]
  {:status 303,
   :template false
   :headers {"Location" (uri user)}})

(defview #'show :html
  [request user]
  {:body (show-section user)})

(defview #'edit :html
  [request user]
  {:body (edit-form user)})

(defview #'update :html
  [request user]
  {:status 302
   :template false
   :headers {"Location" (uri user)}})

(defview #'delete :html
  [request _]
  {:status 303
   :template false
   :headers {"Location" "/admin/users"}})

(defview #'register :html
  [request _]
  {:body
   [:div
    [:h1 "Register"]
    (f/form-to
     [:post "/users"]
     [:p
      (f/label :username "Username:")
      (f/text-field :username)]
     [:p (f/label :password "Password:")
      (f/password-field :password)]
     [:p (f/label :confirm_password "Confirm Password")
      (f/password-field :confirm_password)]
     [:p (f/submit-button "Register")])]})

(defview #'profile :html
  [request user]
  {:body (edit-form user)})
