(ns jiksnu.services
  (:require [jiksnu.app :refer [jiksnu]]
            [jiksnu.registry :as registry]
            [taoensso.timbre :as timbre])
  (:use-macros [gyr.core :only [def.service]]))

(defn pageService
  "Angular service for retrieving pages"
  [$http]
  (let [service #js {}]
    (set! (.-fetch service)
          (fn [page-name]
            (if-let [url (get registry/page-mappings page-name)]
              ;; TODO: cache the page response here and you if-modified for updates
              (-> (.get $http url)
                  (.then (fn [response] (.-data response))))
              (throw (str "page mapping not defined: " page-name)))))
    service))

(set! (.-$inject pageService) #js ["$http"])
(.service jiksnu "pageService" pageService)

(defn subpageService
  "Angular service for retrieving subpages"
  [$q $http]
  (let [service #js {}]
    (set! (.-fetch service)
          (fn [parent page-name]
            (let [type (.getType parent)
                  d (.defer $q)]
              (if-let [mapping-fn (get-in registry/subpage-mappings [type page-name])]
                (let [url (mapping-fn parent)]
                  ;; (timbre/debugf "url: %s" url)
                  (-> $http
                      (.get url)
                      (.success #(.resolve d %))
                      (.error #(.reject d)))
                  (.-promise d))
                (throw (str "Could not find subpage mapping for model "
                            (type parent) " with label " page-name))))))
    service))

(set! (.-$inject subpageService) #js ["$q" "$http"])
(.service jiksnu "subpageService" subpageService)
