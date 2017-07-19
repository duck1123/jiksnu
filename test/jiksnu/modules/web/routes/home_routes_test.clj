(ns jiksnu.modules.web.routes.home-routes-test
  (:require [ciste.formats :refer [format-as]]
            [ciste.model :as cm]
            [jiksnu.mock :as mock]
            [jiksnu.modules.core.db :as db]
            [jiksnu.test-helper :as th]
            [jiksnu.routes-helper :refer [as-user response-for]]
            [midje.sweet :refer :all]
            [ring.mock.request :as req])
  (:import (org.apache.http HttpStatus)))

(th/module-test ["jiksnu.modules.core"
                 "jiksnu.modules.web"])

(fact "route: root/home :get"
  (fact "when there are no activities"
    (db/drop-all!)

    (-> (req/request :get "/") response-for) =>
    (contains {:status HttpStatus/SC_OK}))

  (fact "when there are activities"
    (let [user (mock/a-user-exists)]
      (dotimes [n 10]
        (mock/an-activity-exists {:user user}))

      (fact "when the user is not authenticated"
        (-> (req/request :get "/")
            response-for) =>
        (contains {:status HttpStatus/SC_OK
                   :body string?}))

      (fact "when the user is authenticated"
        (-> (req/request :get "/")
            as-user response-for) =>
        (contains {:status HttpStatus/SC_OK
                   :body string?})))))

(future-fact "route: root/oembed :get"
  (fact "when the format is json"
    (let [activity (mock/an-activity-exists)
          url (str "/main/oembed?format=json&url=" (:url activity))]
      (response-for (req/request :get url)) =>
      (contains {:status HttpStatus/SC_SEE_OTHER
                 :body string?})))

  (fact "when the format is xml"
    (let [activity (mock/an-activity-exists)
          url (str "/main/oembed?format=xml&url=" (:url activity))]
      (response-for (req/request :get url)) =>
      (contains {:status HttpStatus/SC_OK
                 :body string?}))))

(future-fact "route: root/rsd :get"
  (let [response (-> (req/request :get "/rsd.xml") response-for)]
    response => map?
    (:status response) => HttpStatus/SC_OK
    (let [body (cm/string->document (:body response))
          root (.getRootElement body)
          attr {"rsd" "http://archipelago.phrasewise.com/rsd"}
          nodes (cm/query root "//rsd:rsd" attr)]
      (count nodes) => 1)))
