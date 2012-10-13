(ns jiksnu.actions.webfinger-actions
  (:use [ciste.config :only [config]]
        [ciste.core :only [defaction]]
        [ciste.initializer :only [definitializer]]
        [ciste.loader :only [require-namespaces]]
        [clojure.core.incubator :only [-?>]])
  (:require [clojure.tools.logging :as log]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.helpers.user-helpers :as helpers.user]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.model.user :as model.user]
            [jiksnu.model.webfinger :as model.webfinger])
  (:import java.net.URI
           java.net.URL
           jiksnu.model.Domain
           jiksnu.model.User))

(defaction host-meta
  []
  (let [domain (config :domain)
        template (str "http://" domain "/main/xrd?uri={uri}")]
    {:host domain
     :links [{:template template
              :rel "lrdd"
              :title "Resource Descriptor"}]}))

(defaction user-meta
  [uri]
  (->> uri
       model.user/split-uri
       (apply model.user/get-user )))

(defn get-user-meta-uri
  [user]
  (let [domain (model.user/get-domain user)]
    (or (:user-meta-uri user)
        (actions.domain/get-user-meta-url domain (:id user)))))

(defn get-links
  [xrd]
  #_(let [links (force-coll (s/query "//xrd:Link" bound-ns xrd))]
      (map
       (fn [link]
         {:rel (s/query "string(@rel)" bound-ns link)
          :template (s/query "string(@template)" bound-ns link)
          :href (s/query "string(@href)" bound-ns link)
          :lang (s/query "string(@lang)" bound-ns link)})
       links)))

;; TODO: Collect all changes and update the user once.
(defaction update-usermeta
  [user]
  (let [xrd (model.webfinger/fetch-user-meta user)
        links (get-links xrd)
        new-user (assoc user :links links)
        feed (model.user/fetch-user-feed new-user)
        uri (-?> feed .getAuthor .getUri)]
    (doseq [link links]
      (actions.user/add-link user link))
    (-> user
        (assoc :id (str uri))
        (assoc :discovered true)
        ;; TODO: set fields
        actions.user/update)))

(defn discover-webfinger
  [^Domain domain]
  ;; TODO: check https first
  (if-let [xrd (-> domain
                   model.domain/host-meta-link
                   model.webfinger/fetch-host-meta)]
    (if-let [links (get-links xrd)]
      ;; TODO: These should call actions
      (do (model.domain/add-links domain links)
          (model.domain/set-discovered domain))
      (log/error "Host meta does not have any links"))
    (log/error
     (str "Could not find host meta for domain: " (:_id domain)))))

(definitializer
  (require-namespaces
   ["jiksnu.filters.webfinger-filters"
    "jiksnu.views.webfinger-views"]))
