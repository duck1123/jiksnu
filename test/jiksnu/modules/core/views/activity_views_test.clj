(ns jiksnu.modules.web.routes.activity-routes-test
  (:require [ciste.sections.default :refer [full-uri]]
            [clj-factory.core :refer [fseq]]
            [jiksnu.mock :as mock]
            jiksnu.modules.web.routes.activity-routes
            [jiksnu.helpers.routes :refer [as-user json-response response-for]]
            [midje.sweet :refer :all]
            [jiksnu.modules.core.actions.activity-actions :as actions.activity]))

(future-fact "apply-view #'actions.activity/oembed [:http :json]"
  (let [action #'actions.activity/oembed]
    (with-context [:http :json]
      (let [activity (mock/an-activity-exists)
            request {:params {:url (:id activity)}
                     :action action}
            response (filter-action action request)]
        (apply-view request response) =>
        (contains {:status HttpStatus/SC_OK
                   :body (contains {:title (:title activity)})})))))

(future-fact "apply-view #'actions.activity/oembed [:http :xml]"
  (let [action #'actions.activity/oembed]
    (with-context [:http :xml]
      (let [activity (mock/an-activity-exists)
            request {:params {:url (:id activity)}
                     :action action}
            item {} #_(filter-action action request)]
        (let [response (apply-view request item)]
          (let [body (:body response)]
            response => map?
            (:status response) => HttpStatus/SC_OK
            body =not=> string?))))))
