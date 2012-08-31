(ns jiksnu.sections.domain-sections
  (:use [ciste.sections :only [defsection]]
        ciste.sections.default
        [jiksnu.ko :only [*dynamic*]]
        [jiksnu.session :only [current-user is-admin?]]
        [jiksnu.sections :only [action-link admin-index-block
                                admin-index-line control-line]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.sections.link-sections :as sections.link])
  (:import jiksnu.model.Domain))

(defn favicon-link
  [domain]
  [:img
   (if *dynamic*
     {:data-bind "attr: {src: 'http://' + _id + '/favicon.ico'}"}
     {:src (str "http://" (:_id domain) "/favicon.ico")})])

(defn discover-button
  [item]
  (action-link "domain" "discover" (:_id item)))

(defsection actions-section [Domain :html]
  [domain & _]
  [:ul
   [:li (discover-button domain)]
   [:li (delete-button domain)]])

(defsection add-form [Domain :html]
  [domain & _]
  [:form.well {:method "post" :actions "/main/domains"}
   [:fieldset
    [:legend "Add Domain"]
    (control-line "Domain" "domain" "text")
    [:div.actions
     [:button.btn.primary.add-button {:type "submit"}
      "Add"]]]])

(defsection admin-index-block [Domain :viewmodel]
  [items & [page]]
  (->> items
       (map (fn [m] {(:_id m) (admin-index-line m page)}))
       (into {})))

(defsection delete-button [Domain :html]
  [item & _]
  (action-link "domain" "delete" (:_id item)))

(defsection index-block [Domain :html]
  [domains & _]
  [:table.domains.table
   [:thead
    [:tr
     [:th "Name"]
     [:th "XMPP?"]
     [:th "Discovered"]
     [:th "Host Meta"]
     [:th "# Links"]
     #_[:th "Actions"]]]
   [:tbody (when *dynamic* {:data-bind "foreach: $data"})
    (map index-line domains)]])

(defsection index-block [Domain :viewmodel]
  [items & [page]]
  (->> items
       (map (fn [m] (index-line m page)))
       doall))

(defsection index-line [Domain :html]
  [domain & _]
  [:tr {:data-model "domain"}
   [:td
    (favicon-link domain)
    (link-to domain)]
   [:td (if *dynamic*
          {:data-bind "text: xmpp"}
          (:xmpp domain))]
   [:td (if *dynamic*
          {:data-bind "text: discovered"}
          (:discovered domain))]
   [:td
    [:a
     (if *dynamic*
       {:data-bind "attr: {href: 'http://' + _id + '/.well-known/host-meta'}"}
       {:href (str "http://" (:_id domain) "/.well-known/host-meta")})
     "Host-Meta"]]
   [:td
    (if *dynamic*
      {:data-bind "text: links.length"}
      (count (:links domain)))]
   [:th (actions-section domain)]])

(defsection link-to [Domain :html]
  [domain & _]
  [:a (if *dynamic*
        {:data-bind "attr: {href: '/main/domains/' + _id}, text: _id"}
        {:href (uri domain)})
   (when-not *dynamic*
     (:_id domain))])

;; show-section

(defsection show-section [Domain :html]
  [domain & _]
  [:div {:data-model "domain"}
   [:p "Id: "
    (favicon-link domain)
    [:span.domain-id (:_id domain)]]
   [:p "XMPP: " (:xmpp domain)]
   [:p "Discovered: " (:discovered domain)]
   (when-let [sc (:statusnet-config domain)]
     (list [:p "Closed: " (-> sc :site :closed)]
           [:p "Private: " (-> sc :site :private)]
           [:p "Invite Only: " (-> sc :site :inviteonly)]
           [:p "Admin: " (-> sc :site :email)]
           (when-let [license (:license sc)]
             [:p "License: "
              ;; RDFa
              [:a {:href (:url license)
                   :title (:title license)}
               [:img {:src (:image license)
                      :alt (:title license)}]]])))
   (when (is-admin?)
     (actions-section domain))
   (when (seq (:links domain))
     (sections.link/index-section (:links domain)))
   (when (current-user) (discover-button domain))])

(defsection show-section [Domain :model]
  [item & [page]]
  item)

(defsection show-section [Domain :viewmodel]
  [item & [page]]
  item)

;; uri

(defsection uri [Domain]
  [domain & _]
  (str "/main/domains/" (:_id domain)))
