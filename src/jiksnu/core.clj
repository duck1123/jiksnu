(ns jiksnu.core
  (:require [jiksnu.db :as db]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.actions.subscription-actions :as actions.subscription]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.channels :as ch]
            [jiksnu.model.feed-source :as model.feed-source]
            [jiksnu.model.resource :as model.resource]
            [jiksnu.model.user :as model.user]
            jiksnu.modules.core.triggers.activity-triggers
            jiksnu.modules.core.triggers.conversation-triggers
            jiksnu.modules.core.triggers.domain-triggers
            jiksnu.workers
            [manifold.bus :as bus]
            [manifold.stream :as s]))

(defn start
  []
  #_(timbre/info "starting core")

  (db/set-database!)
  (actions.subscription/setup-delete-hooks)

  #_(bus/publish! ch/events :activity-posted {:msg "activity posted"})

  (->> (bus/subscribe ch/events :activity-posted)
       (s/consume actions.subscription/handle-follow-activity))

  ;; (model.activity/ensure-indexes)
  (model.feed-source/ensure-indexes)
  (model.resource/ensure-indexes)
  (model.user/ensure-indexes)

  ;; cascade delete on domain deletion
  (dosync
   (alter actions.user/delete-hooks conj #'actions.activity/handle-delete-hook)))

(defn stop [])
