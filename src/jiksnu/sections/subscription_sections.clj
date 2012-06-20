(ns jiksnu.sections.subscription-sections
  (:use [ciste.model :only [implement]]
        [ciste.sections :only [defsection]]
        [ciste.sections.default :only [delete-button full-uri uri title index-line
                                       index-section link-to show-section]]
        [jiksnu.sections :only [control-line]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.model.subscription :as model.subscription]
            [jiksnu.model.user :as model.user]
            [jiksnu.sections.user-sections :as sections.user])
  (:import jiksnu.model.Subscription))

;; (defsection title [Subscription]
;;   [subscription & _]
;;   (str (:_id subscription)))

(defn show-minimal
  [user]
  [:span.vcard
   (sections.user/display-avatar user 32)
   [:span.fn.n.minimal-text (:display-name user)]])


(defn subscriber-line
  [subscription]
  [:li {:id (str "subscription-" (:_id subscription))}
   (-> subscription model.subscription/get-actor show-minimal)])

(defn subscriptions-line
  [subscription]
  [:li {:id (str "subscription-" (:_id subscription))}
   (-> subscription model.subscription/get-target show-minimal)])

(defsection uri [Subscription]
  [subscription & _]
  (str "/admin/subscriptions/" (:_id subscription)))

;; (defsection delete-button [Subscription :html]
;;   [subscription & _]
;;   [:form {:method "post"
;;           :action (str "/main/subscriptions/" (:_id subscription) "/delete")}
;;    [:button.btn {:type "submit"}
;;     [:i.icon-trash] [:span.button-text "Delete"]]])

(defn subscribers-section
  [user]
  (when user
    (let [subscriptions (model.subscription/subscribers user)]
      [:div.subscribers
       [:h3
        ;; subscribers link
        [:a {:href (str (full-uri user) "/subscribers")} "Followers"] " " (count subscriptions)]
       [:ul.unstyled
        [:li (map subscriber-line subscriptions)]]])))

(defn subscriptions-section
  [user]
  (when user
    (let [subscriptions (model.subscription/subscriptions user)]
      [:div.subscriptions
       [:h3
        [:a {:href (str (full-uri user) "/subscriptions")} "Following"] " " (count subscriptions)]
       [:ul (map subscriptions-line subscriptions)]
       [:p
        [:a {:href "/main/ostatussub"} "Add Remote"]]])))

(defn ostatus-sub-form
  []
  [:form {:method "post"
          :action "/main/ostatussub"}
   (control-line "Username"
                 "profile" "text")
   [:div.actions
    [:input.btn.primary {:type "submit" :value "Submit"}]]])

(defn subscribers-index
  [subscriptions]
  (index-section
   (map model.subscription/get-target subscriptions)))

(defn subscriptions-index
  [subscriptions]
  (index-section
   (map model.subscription/get-target subscriptions)))

(defn subscriptions-index-json
  [subscriptions]
  (implement))

(defn admin-index-section
  [subscriptions]
  [:table.table
   [:thead
    [:tr
     [:th "id"]
     [:th "actor"]
     [:th "target"]
     [:th "Created"]
     [:th "pending"]
     [:th "local"]
     [:th "Delete"]]]
   [:tbody
    (map
     (fn [subscription]
       [:tr
        [:td (link-to subscription)]
        [:td (let [user (model.subscription/get-actor subscription)]
               (link-to user))]
        [:td (let [user (model.subscription/get-target subscription )]
               (link-to user))]
        [:td (:created subscription)]
        [:td (:pending subscription)]
        [:td (:local subscription)]
        [:td (delete-button subscription)]])
     subscriptions)]])

(defsection index-line [Subscription :as]
  [subscription & _]
  (let [actor (model.subscription/get-actor subscription)
        target (model.subscription/get-target subscription)]
    {:verb "follow"
     :actor (show-section actor)
     :target (show-section target)}))
