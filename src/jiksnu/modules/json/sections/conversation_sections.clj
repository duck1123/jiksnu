(ns jiksnu.modules.json.sections.conversation-sections
  (:require [ciste.sections :refer [defsection]]
            [ciste.sections.default :refer [uri index-block index-section]])
  (:import jiksnu.model.Conversation))

(defsection index-block [Conversation :json]
  [items & [page]]
  (map identity (:items page)))

(defsection index-section [Conversation :json]
  [items & [page]]
  {"@type" "http://activitystrea.ms/2.0/Collection"
   :totalItems (:totalItems page)
   :itemsPerPage (:page-size page)
   :items (index-block items page)
   :page (:page page)})
