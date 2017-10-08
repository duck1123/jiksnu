(ns jiksnu.app.services
  (:require [jiksnu.app :as a]
            [jiksnu.registry :as registry]))

(defn fetch-page
  [$http page-name]
  (if-let [url (get registry/page-mappings page-name)]
    ;; TODO: cache the page response here and you if-modified for updates
    (-> (.get $http url)
        (.then (fn [response] (.-data response))))
    (throw (str "page mapping not defined: " page-name))))

(defn fetch-sub-page
  [$http parent page-name]
  (let [type (.getType parent)]
    (when-let [mapping-fn (get-in registry/subpage-mappings [type page-name])]
      (let [url (mapping-fn parent)]
        (-> (.get $http url)
            (.then (fn [response] (.-data response))))))))

(defn pageService
  "Angular service for retrieving pages"
  [$http]
  #js {:fetch #(fetch-page $http %)})

(defn subpageService
  "Angular service for retrieving subpages"
  [$http]
  #js {:fetch #(fetch-sub-page $http %1 %2)})

(.service a/jiksnu "pageService" #js ["$http" pageService])
(.service a/jiksnu "subpageService" #js ["$http" subpageService])
