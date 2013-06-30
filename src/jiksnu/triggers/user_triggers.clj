(ns jiksnu.triggers.user-triggers
  (:use [ciste.triggers :only [add-trigger!]]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-tigase.core :as tigase]
            [clj-tigase.element :as element]
            [clojure.tools.logging :as log]
            [jiksnu.namespace :as ns]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.channels :as ch]
            [jiksnu.model.user :as model.user]
            [jiksnu.ops :as ops]
            [jiksnu.util :as util]
            [lamina.core :as l]))

;; (defn fetch-updates-trigger
;;   [action _ user]
;;   (let [domain (model.user/get-domain user)]
;;     (when (:xmpp domain) (fetch-updates-xmpp user))
;;     #_(fetch-updates-http user)))

(defn parse-avatar
  [user link]
  (when (= (first (:extensions link)) "96")
    (model.user/set-field! user :avatarUrl (:href link))))

(defn parse-updates-from
  [user link]
  (log/debug "Setting update source")
  (if-let [href (:href link)]
    (let [source (actions.feed-source/find-or-create {:topic href})]
      (model.user/set-field! user :update-source (:_id source)))
    (throw+ "link must have a href")))

(defn add-link-trigger
  [action [user link] _]
  (condp = (:rel link)
    ;; "magic-public-key" (parse-magic-public-key user link)
    "avatar" (parse-avatar user link)
    ns/updates-from (parse-updates-from user link)
    nil))

(defn create-trigger
  [action _ user]
  (actions.user/discover user))

(add-trigger! #'actions.user/add-link*     #'add-link-trigger)
(add-trigger! #'actions.user/create        #'create-trigger)
;; (add-trigger! #'actions.user/fetch-updates #'fetch-updates-trigger)

(defn handle-pending-get-user-meta*
  [user]
  (actions.user/get-user-meta user))

(def handle-pending-get-user-meta
  (ops/op-handler handle-pending-get-user-meta*))

(defn init-receivers
  []

  (l/receive-all ch/pending-get-user-meta
                 handle-pending-get-user-meta)

  )

(defn init-hooks
  []
  (util/add-hook!
   actions.domain/delete-hooks
   (fn [domain]
     (doseq [user (:items (model.user/fetch-by-domain domain))]
       (actions.user/delete user))
     domain)))

(defonce receivers (init-receivers))
(defonce hooks (init-hooks))
