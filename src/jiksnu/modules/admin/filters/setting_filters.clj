(ns jiksnu.modules.admin.filters.setting-filters
  (:use [ciste.filters :only [deffilter]]
        [jiksnu.modules.admin.actions.setting-actions :only [edit-page update-settings]]))

(deffilter #'edit-page :http
  [action request]
  (action))

(deffilter #'update-settings :http
  [action request]
  (action (:params request)))
