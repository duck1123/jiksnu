(ns jiksnu.modules.core.views.feed-source-views
  (:require [ciste.views :refer [defview]]
            [ciste.sections.default :refer [show-section]]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.modules.core.sections :refer [format-page-info]]))

(defview #'actions.feed-source/index :page
  [request response]
  (let [items (:items response)
        response (merge response
                        {:id (:name request)
                         :items (map :_id items)})]
    {:body {:action "page-updated"
            :body response}}))
