(ns jiksnu.modules.web.routes.subscription-routes-test
  (:require [clj-factory.core :refer [fseq]]
            [taoensso.timbre :as log]
            [clojurewerkz.support.http.statuses :as status]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.mock :as mock]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.user :as model.user]
            [jiksnu.ops :as ops]
            [jiksnu.routes-helper :refer [as-user response-for]]
            [jiksnu.test-helper :as th]
            [manifold.deferred :as d]
            [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :as req]))

(namespace-state-changes
 [(before :contents (th/setup-testing))
  (after :contents (th/stop-testing))])

(future-fact "ostatus submit"
  (let [username (fseq :username)
        domain-name (fseq :domain)
        uri (format "acct:%s@%s" username domain-name)
        params {:profile uri}]

    (fact "when not authenticated"
      (fact "when it is a remote user"
        (let [response (-> (req/request :post "/main/ostatussub")
                           (assoc :params params)
                           response-for)]
          response => map?
          (:status response) => status/redirect?)))

    (fact "when authenticated"
      (let [actor (mock/a-user-exists)]
        (fact "when it is a remote user"
          (-> (req/request :post "/main/ostatussub")
              (assoc :params params)
              (as-user actor)
              response-for) =>
              (contains {:status status/redirect?})
              (provided
                (ops/get-discovered anything) => (d/success-deferred
                                                  (model/map->Domain {:_id domain-name}))))))))

(future-fact "get-subscriptions"
  (let [user (mock/a-user-exists)
        subscription (mock/a-subscription-exists {:from user})
        path (str "/users/" (:_id user) "/subscriptions")]
    (-> (req/request :get path)
        response-for) =>
        (contains {:status status/success?
                   :body #(enlive/select (th/hiccup->doc %) [:.subscriptions])})))
