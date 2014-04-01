(ns jiksnu.routes.resource-routes
  (:require [ciste.initializer :refer [definitializer]]
            [ciste.loader :refer [require-namespaces]]
            [jiksnu.actions.resource-actions :refer [delete discover index show update]]
            [jiksnu.routes.helpers :refer [add-route! named-path formatted-path]]))

(add-route! "/resources"              {:named "index resources"})
(add-route! "/resources/:id"          {:named "show resource"})
(add-route! "/resources/:id/delete"   {:named "delete resource"})
(add-route! "/resources/:id/discover" {:named "discover resource"})
(add-route! "/resources/:id/update"   {:named "update resource"})
(add-route! "/model/resources/:id"    {:named "resource model"})

(defn routes
  []
  [
   [[:get    (formatted-path "index resources")]   #'index]
   [[:get    (named-path     "index resources")]   #'index]
   [[:get    (formatted-path "show resource")]     #'show]
   [[:get    (named-path     "show resource")]     #'show]
   [[:post   (formatted-path "discover resource")] #'discover]
   [[:post   (named-path     "discover resource")] #'discover]
   [[:post   (formatted-path "update resource")]   #'update]
   [[:post   (named-path     "update resource")]   #'update]
   [[:delete (named-path     "show resource")]     #'delete]
   [[:post   (named-path     "delete resource")]   #'delete]
   [[:get    (formatted-path "resource model")]    #'show]
   ])

(defn pages
  []
  [
   [{:name "resources"}    {:action #'index}]
   ])


(definitializer
  (require-namespaces
   ["jiksnu.handlers.atom"
    "jiksnu.handlers.html"
    "jiksnu.handlers.xrd"
    ]))
