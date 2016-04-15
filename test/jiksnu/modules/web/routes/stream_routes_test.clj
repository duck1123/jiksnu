(ns jiksnu.modules.web.routes.stream-routes-test
  (:require [clj-factory.core :refer [factory fseq]]
            [clojure.data.json :as json]
            [jiksnu.db :as db]
            [jiksnu.mock :as mock]
            jiksnu.modules.web.routes.stream-routes
            [jiksnu.test-helper :as th]
            [jiksnu.routes-helper :refer [as-user response-for]]
            [midje.sweet :refer :all]
            [ring.mock.request :as req]))

(th/module-test ["jiksnu.modules.core"
                 "jiksnu.modules.web"])

(fact "route: streams-api/collection :post"
  (db/drop-all!)
  (let [params {:name (fseq :word)}
        actor (mock/a-user-exists)
        request (-> (req/request :post "/model/streams")
                    (req/body (json/json-str params))
                    (req/content-type "application/json")
                    (as-user actor))
        response (response-for request)
        location (get-in response [:headers "Location"])
        request2 (req/request :get location)]

    response =>
    (contains {:status 303
               :headers (contains {"Location" #"/model/streams/[\d\w]+"})})

    (some-> request2 response-for :body json/read-str) =>
    (contains {"name" (:name params)
               "owner" (:_id actor)})))

(fact "route: streams-api/collection :delete"
  (let [stream (mock/a-stream-exists)
        url (str "/model/streams/" (:_id stream))
        request (-> (req/request :delete url))]
    (response-for request) => (contains {:status 200})))

(fact "route: streams-api/activities :get"
  (db/drop-all!)
  (let [stream (mock/a-stream-exists)
        url (str "/model/streams/" (:_id stream) "/activities")
        request (-> (req/request :get url))]
    (mock/there-is-an-activity {:stream stream})

    (let [response (response-for request)]
      ;; (json/read-string (:body response)) => {}
      response  => (contains {:status 200})
      (let [body (json/read-json (:body response))]
        body => (contains {:totalItems 1})))))
