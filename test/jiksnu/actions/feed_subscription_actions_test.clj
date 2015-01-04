(ns jiksnu.actions.feed-subscription-actions-test
  (:require [clj-factory.core :refer [factory fseq]]
            [clojure.tools.logging :as log]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.actions.feed-subscription-actions :as actions.feed-subscription]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.factory :refer [make-uri]]
            [jiksnu.mock :as mock]
            [jiksnu.model.feed-subscription :as model.feed-subscription]
            [jiksnu.test-helper :as th]
            [midje.sweet :refer [=> after before fact falsey
                                 namespace-state-changes]])
  (:import jiksnu.model.FeedSubscription))

(namespace-state-changes
 [(before :contents (th/setup-testing))
  (after :contents (th/stop-testing))])

(fact "#'actions.feed-subscription/delete"
  (let [item (mock/a-feed-subscription-exists)]
    (actions.feed-subscription/delete item)

    (actions.feed-subscription/exists? item) => falsey))

(fact "#'actions.feed-subscription/create"
  (let [params (factory :feed-subscription)
        params (actions.feed-subscription/prepare-create params)]
    (let [response (actions.feed-subscription/create params)]
      response => (partial instance? FeedSubscription))))

(fact "#'actions.feed-subscription/index"
  (model.feed-subscription/drop!)
  (let [response (actions.feed-subscription/index)]
    (:items response) => []))

(fact "#'actions.feed-subscription/subscription-request"
  (let [topic (fseq :uri)
        source (mock/a-feed-source-exists {:local true})
        params {:callback (fseq :uri)
                :verify-token (fseq :verify-token)
                :lease-seconds (fseq :lease-seconds)
                :secret (fseq :secret-key)
                :topic (:topic source)}]
    (let [response (actions.feed-subscription/subscription-request params)]
      response => (partial instance? FeedSubscription))))


