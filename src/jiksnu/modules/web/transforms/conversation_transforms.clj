(ns jiksnu.modules.web.transforms.conversation-transforms
  (:require [ciste.config :only [config]]
            [clojure.tools.logging :as log]
            [jiksnu.actions.domain-actions :as actions.domain]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.actions.resource-actions :as actions.resource]
            [jiksnu.routes.helpers :as rh]
            [jiksnu.util :as util]
            [lamina.trace :as trace]
            [slingshot.slingshot :only [throw+]])
  (:import java.net.URI))

(defn set-url
  [item]
  (if (:url item)
    item
    (when (:local item)
      (assoc item :url ""))))

