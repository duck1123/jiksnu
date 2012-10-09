(ns jiksnu.routes.conversation-routes
  (:use [clojurewerkz.route-one.core :only [add-route! named-path]]
        [jiksnu.routes.helpers :only [formatted-path]])
  (:require [jiksnu.actions.conversation-actions :as conversation]))

(add-route! "/main/conversations"     {:named "index conversations"})
(add-route! "/main/conversations/:id" {:named "show conversation"})
(add-route! "/model/conversations/:id" {:named "conversation model"})

(defn routes
  []
  [[[:get (named-path     "index conversations")] #'conversation/index]
   [[:get (formatted-path "index conversations")] #'conversation/index]
   [[:get (named-path     "show conversation")]  #'conversation/show]
   [[:get (formatted-path "show conversation")]  #'conversation/show]
   [[:get (formatted-path "conversation model")] #'conversation/show]
   ])
