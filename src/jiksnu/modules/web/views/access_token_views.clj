(ns jiksnu.modules.web.views.access-token-views
  (:require [ciste.sections.default :refer [link-to]]
            [ciste.views :refer [defview]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [jiksnu.actions.access-token-actions :as actions.access-token]
            [jiksnu.ko :refer [*dynamic*]]
            [jiksnu.modules.web.sections :refer [bind-to dump-data pagination-links with-page]]
            [ring.util.response :as response]))

(deffilter #'actions.access-token/get-access-token :text
  [request response]
  {:body
   {}
   }
  )
