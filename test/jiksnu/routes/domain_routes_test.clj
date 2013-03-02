(ns jiksnu.routes.domain-routes-test
  (:use [ciste.core :only [with-context]]
        [ciste.sections.default :only [uri]]
        [clj-factory.core :only [factory]]
        [jiksnu.routes-helper :only [response-for]]
        [jiksnu.test-helper :only [test-environment-fixture]]
        [midje.sweet :only [fact future-fact => every-checker contains]])
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.support.http.statuses :as status]
            [hiccup.core :as h]
            [jiksnu.mock :as mock]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.domain :as model.domain]
            [ring.mock.request :as req]))

(test-environment-fixture

 (fact "show"
   (with-context [:http :html]
     (let [domain (mock/a-domain-exists)]
       (-> (req/request :get (uri domain))
           response-for) =>
           (every-checker
            map?
            (comp status/success? :status)
            (fn [response]
              (let [body (h/html (:body response))]
                (fact
                  body => (re-pattern (str (:_id domain))))))))))

 (fact "#'webfinger-host-meta"
   (fact "should return a XRD document"
     (-> (req/request :get "/.well-known/host-meta")
         response-for) =>
         (every-checker
          map?
          (comp status/success? :status)
          (fn [req]
            (let [body (:body req)]
              (fact
                body => #"<XRD.*"))))))
 )
