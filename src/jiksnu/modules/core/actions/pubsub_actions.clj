(ns jiksnu.modules.core.actions.pubsub-actions
  (:require [jiksnu.modules.core.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.modules.core.actions.feed-subscription-actions :as actions.feed-subscription]
            [jiksnu.modules.core.model.feed-source :as model.feed-source]
            [jiksnu.util :as util]
            [org.httpkit.client :as client]
            [slingshot.slingshot :refer [throw+]])
  (:import (org.apache.http HttpStatus)))

(defn verify-subscribe-sync
  "Verify subscription request in this thread"
  [feed-subscription params]
  (if-let [callback (:callback params)]
    (if (:url feed-subscription)
      ;; sending verification request
      (let [params {:hub.mode          "subscribe"
                    :hub.topic         (:url feed-subscription)
                    :hub.lease_seconds (:lease-seconds feed-subscription)
                    :hub.challenge     (:challenge feed-subscription)
                    :hub.verify_token  (:verify-token feed-subscription)}
            url (util/make-subscribe-uri (:callback feed-subscription) params)
            ;; TODO: handle this in resources?
            response-p (client/get url)]
        ;; NB: This blocks
        (let [response @response-p]
          (if (= HttpStatus/SC_OK (:status response))
            {:status HttpStatus/SC_NO_CONTENT}
            {:status HttpStatus/SC_NOT_FOUND})))
      (throw+ "feed subscription is not valid"))
    (throw+ "Could not determine callback url")))

(defn verify-subscription-async
  "asynchronous verification of hub subscription"
  [subscription params]
  (verify-subscribe-sync subscription params))

(defn subscribe
  "Set up a remote subscription to a local source"
  [params]
  (let [subscription (actions.feed-subscription/subscription-request params)
        dispatch-fn (if (= (:verify params) "async")
                      verify-subscription-async
                      verify-subscribe-sync)]
    (dispatch-fn subscription params)))

(defn unsubscribe
  "Remove a remote subscription to a local source"
  [params]
  ;; TODO: This should be doing a fsub removal
  (if-let [subscription (model.feed-source/find-record {:topic (:topic params)
                                                        :callback (:callback params)})]
    (actions.feed-source/unsubscribe subscription)
    (throw+ "subscription not found")))

(defn hub-dispatch
  "Handle pubsub requests against hub endpoint"
  [params]
  (condp = (:mode params)
    "subscribe"   (subscribe params)
    "unsubscribe" (unsubscribe params)
    (throw+ "Unknown mode type")))
