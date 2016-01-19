(ns jiksnu.modules.web
  (:require [ciste.loader :refer [defmodule]]
            [jiksnu.modules.http.resources :refer [init-site-reloading!]]
            jiksnu.modules.web.formats
            [jiksnu.modules.web.handlers :as handlers]
            [jiksnu.modules.web.helpers :as helpers]
            [jiksnu.modules.web.core :refer [jiksnu-init]]
            [taoensso.timbre :as timbre]))

(defn start
  []
  ;; (timbre/info "starting web")
  (handlers/init-handlers)
  (helpers/load-routes)
  (init-site-reloading! jiksnu-init)
  (jiksnu-init))

(defmodule "jiksnu.modules.web"
  :start start
  :deps ["jiksnu.modules.core"
         "jiksnu.modules.json"])
