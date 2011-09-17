(ns jiksnu.model.push-subscription
  (:use (ciste [debug :only (spy)])
        jiksnu.model
        [jiksnu.session :only (current-user current-user-id is-admin?)])
  (:require [clojure.string :as string]
            [karras.entity :as entity]
            [karras.sugar :as sugar])
  (:import jiksnu.model.PushSubscription))

(defn create
  [options]
  (let [now (sugar/date)]
    (entity/create
     PushSubscription
     (merge
      {:created now
       :update now}
      options))))

(defn fetch
  [options]
  (entity/fetch-one PushSubscription options))

(defn find-or-create
  [options]
  (or (fetch options)
      (create options)))

(defn index
  []
  (entity/fetch PushSubscription {}))
