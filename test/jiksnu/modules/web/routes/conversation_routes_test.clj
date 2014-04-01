(ns jiksnu.modules.web.routes.conversation-routes-test
  (:require [clj-factory.core :refer [factory]]
            [clojure.tools.logging :as log]
            [clojurewerkz.support.http.statuses :as status]
            jiksnu.modules.web.views.conversation-views
            [jiksnu.routes.helpers :refer [named-path]]
            [jiksnu.routes-helper :refer [response-for]]
            [jiksnu.test-helper :refer [check context test-environment-fixture]]
            [midje.sweet :refer [=>]]
            [ring.mock.request :as req]))

(test-environment-fixture

 (context "index page"
   (->> (named-path "index conversations")
        (req/request :get)
        response-for) =>
        (check [response]
          response => map?
          (:status response) => status/success?
          (:body response) => string?
          ))

 (context "index page (:viewmodel)"
   (->> (str (named-path "index conversations") ".viewmodel")
        (req/request :get)
        response-for) =>
        (check [response]
          response => map?
          (:status response) => status/success?
          (:body response) => string?))

 )
