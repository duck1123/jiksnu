(ns jiksnu.modules.admin.routes.like-routes-test
  (:require [clj-factory.core :refer [factory]]
            [clojure.tools.logging :as log]
            [clojurewerkz.support.http.statuses :as status]
            [jiksnu.actions.auth-actions :as actions.auth]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model :as model]
            [jiksnu.model.like :as model.like]
            [jiksnu.model.user :as model.user]
            [jiksnu.routes-helper :refer [as-admin response-for]]
            [jiksnu.test-helper :as th]
            [midje.sweet :refer :all]
            [ring.mock.request :as req]
            [slingshot.slingshot :refer [throw+]]))

(namespace-state-changes
 [(before :contents (th/setup-testing))
  (after :contents (th/stop-testing))])

(future-fact "delete"
  (let [like (model.like/create (factory :like))
        url (str "/admin/likes/" (:_id like) "/delete")]
    (let [response (-> (req/request :post url)
                       as-admin response-for)]
      response => map?
      (:status response) => status/redirect?)))

