(ns jiksnu.sections.activity-sections
  (:use (ciste [core :only [with-format]]
               [debug :only [spy]]
               [sections :only [defsection]])
        (ciste.sections [default :only [index-section]])
        (clojure.core [incubator :only [-?>]])
        (plaza.rdf core)
        (plaza.rdf.vocabularies foaf))
  (:require (hiccup [core :as h])
            (jiksnu [abdera :as abdera]
                    [model :as model]
                    [namespace :as ns]
                    [session :as session]
                    [view :as view])
            (jiksnu.actions [user-actions :as actions.user])
            (jiksnu.helpers [activity-helpers :as helpers.activity]
                            [user-helpers :as helpers.user])
            (jiksnu.model [activity :as model.activity]
                          [user :as model.user])
            (jiksnu.sections [user-sections :as sections.user])
            (jiksnu.xmpp [element :as element])
            (ring.util [codec :as codec]))
  (:import com.ocpsoft.pretty.time.PrettyTime
           java.io.StringWriter
           javax.xml.namespace.QName
           jiksnu.model.Activity))

;; TODO: Move to common area
(register-rdf-ns :aair ns/aair)
(register-rdf-ns :as ns/as)
(register-rdf-ns :dc ns/dc)

;; model?
(defn get-author
  [activity]
  (-> activity
      :author
      model.user/fetch-by-id))


;; specific sections

(defn pictures-section
  [activity]
  [:div.pictures-line.control-group
   [:label.control-label {:for "pictures"} "Pictures"]
   [:div.controls
    [:input {:type "file" :name "pictures"}]]])

(defn tag-section
  [activity]
  [:div.control-group
   [:label.control-label {:for "tags"} "Tags"]
   [:div.controls
    [:input {:type "text" :name "tags"}]
    [:a.btn {:href "#"} "Add Tags"]]])

(defn location-section
  [activity]
  [:div.control-group
   [:label.control-label "Location"]
   [:div.controols
    [:label {:for "lat"} "Latitude"]
    [:div.input
     [:input {:type "text" :name "lat"}]]
    [:label {:for "long"} "Longitude"]
    [:div.input
     [:input {:type "text" :name "lat"}]]]])


(defn add-button-section
  [activity]
  [:fieldset.add-buttons
   [:legend "Add:"]
   [:ul.btn-group
    [:li [:a.btn {:href "#"} "Tags"]]

    [:li [:a.btn {:href "#"} "Recipients"]]

    [:li [:a.btn {:href "#"} "Location"]]

    [:li [:a.btn {:href "#"} "Links"]]

    [:li [:a.btn {:href "#"} "Pictures"]]]])

(defn privacy-select
  [activity]
  [:select {:name "privacy"}
   [:option {:value "public"} "Public"]
   [:option {:value "group"} "Group"]
   [:option {:value "custom"} "Custom"]
   [:option {:value "private"} "Private"]])

(defn type-line
  [activity]
  [:div.type-line
   [:ul.nav.nav-tabs
    [:li
     [:a {:href "#note" :data-toggle "tab"} "Note"]]
    [:li
     [:a {:href "#status" :data-toggle "tab"} "Status"]]
    [:li
     [:a {:href "#checkin" :data-toggle "tab"} "Checkin"]]
    [:li
     [:a {:href "#picture" :data-toggle "tab"} "Picture"]]
    [:li
     [:a {:href "#event" :data-toggle "tab"} "Event"]]
    [:li
     [:a {:href "#bookmark" :data-toggle "tab"} "Bookmark"]]
    [:li
     [:a {:href "#question" :data-toggle "tab"} "Question"]]]])

(defn like-button
  [activity]
  [:form {:method "post" :action (str "/notice/" (:_id activity) "/like")}
   [:button.btn {:type "submit"}
    [:i.icon-heart] [:span.button-text "like"]]])

(defn update-button
  [activity]
  [:form {:method "post" :action (str "/notice/" (:_id activity) "/update")}
   [:button.btn {:type "submit"}
    [:i.icon-refresh] [:span.button-text "update"]]])

(defn edit-button
  [activity]
  [:form {:method "post" :action (str "/notice/" (:_id activity) "/edit")}
   [:button.btn {:type "submit"}
    [:i.icon-edit] [:span.button-text "edit"]]])

(defn author?
  [activity user]
  (= (:author activity) (:_id user)))

(defn post-actions
  [activity]
  (let [authenticated (session/current-user)]
    [:div.pull-right
     [:ul.post-actions.unstyled.buttons
      (when authenticated
        (list [:li (like-button activity)]
              [:li [:a.btn {:href "#"} [:i.icon-comment] [:span.button-text "Comment"]]]))
      (when (or (author? activity authenticated)
                (session/is-admin?))
        (list [:li (edit-button activity)]
              [:li (delete-button activity)]))
      (when (session/is-admin?)
        [:li (update-button activity)])]]))

(defn recipients-section
  [activity]
  (list
   (when (:irts activity)
     [:p "In reply to: " (:irts activity)])
   (when (:mentioned-uri activity)
     [:p "mentioned: " (:mentioned-uri activity)])))

(defn links-section
  [activity]
  
  )

(defn maps-section
  [activity]
  
  (if (and (seq (:lat activity))
           (seq (:long activity)))
    [:div.map-section
     [:img.map
      {:src
       (str "https://maps.googleapis.com/maps/api/staticmap?size=200x200&zoom=11&sensor=true&markers=color:red|"
            (:lat activity)
            ","
            (:long activity))}]
     #_[:p "Lat: " (:lat activity)]
     #_[:p "Long: " (:long activity)]]))

(defn likes-section
  [activity]
  (when (:likes activity)
    [:section
     [:span "Liked by"]
     [:ul
      (map
       (fn [user]
         [:li (link-to user)])
       (:likes activity))]]))

(defn tags-section
  [activity]
  (when (:tags activity)
    [:div.tags
     [:span "Tags: "]
     [:ul.tags
      (map
       (fn [tag]
         [:li [:a {:href (str "/tags/" tag) :rel "tag"} tag]])
       (:tags activity))]]))

(defn posted-link-section
  [activity]
  [:span.posted
   "posted "
   [:time {:datetime (:published activity)
           :title (:published activity)}
    [:a {:href (uri activity)
         :property "dc:published"
         :content (:published activity)}
     (-> activity :published model.activity/prettyify-time)]]])

(defn comments-section
  [activity]
  (when (and (:comment-count activity)
             (> (:comment-count activity) 0))
    [:section.comments
     [:h4.hidden "Comments"]]))

(defn question-form
  [activity]
  
  )

(defn activity-form
  ([] (activity-form (Activity.)))
  ([activity]
     (let [{:keys [id parent-id content title]} activity]
       [:div.post-form
        [:form {:method "post"
                :action "/notice/new"
                :enctype "multipart/form-data"}
         [:fieldset
          [:legend "Post an activity"]
          (when (:id activity)
            [:div.control-group
             [:input {:type "hidden" :name "_id" :value id}]])
          (when parent-id
            [:div.control-group
             [:input {:type "hidden" :name "parent" :value parent-id}]])
          (type-line activity)
          [:div.control-group
           [:label.control-label {:for "title"} "Title"]
           [:div.controls
            [:input {:type "text" :name "title" :value title}]]]
          [:div.control-group
           [:label.control-label {:for "content"} "Content"]
           [:div.controls
            [:textarea.span6 {:name "content" :rows "3"} content]]]

          (pictures-section activity)
          (location-section activity)
          (tag-section activity)
          (add-button-section activity)
          [:div.actions
           (privacy-select activity)
           [:input.btn.btn-primary.pull-right {:type "submit" :value "post"}]]]]])))

;; dynamic sections


(defsection title [Activity]
  [activity & options]
  (:title activity))

(defsection uri [Activity]
  [activity & options]
  (str "/notice/" (:_id activity)))

(defsection delete-button [Activity :html]
  [activity & _]
  [:form {:method "post" :action (str "/notice/" (:_id activity))}
   [:input {:type "hidden" :name "_method" :value "DELETE"}]
   [:button.btn {:type "submit"}
    [:i.icon-trash] [:span.button-text "Delete"]]])



(defsection index-block [Activity :html]
  [activities & _]
  (index-section activities))

(defsection index-block [Activity :xml]
  [activities & _]
  [:statuses {:type "array"}
   (map index-line activities)])

(defsection index-block [Activity :xmpp]
  [activities & options]
  ["items" {"node" ns/microblog}
   (map index-line activities)])





(defsection index-line [Activity]
  [activity & opts]
  (apply show-section activity opts))

(defsection index-line [Activity :html]
  [activity & opts]
  [:li (apply show-section activity opts)])

(defsection index-line [Activity :xmpp]
  [^Activity activity & options]
  ["item" {"id" (:_id activity)}
   (show-section activity)])




(defsection index-section [Activity :html]
  [activities & _]
  (list
   [:ul.unstyled
    (map index-line activities)]
   [:ul.pager
    [:li.previous [:a {:href "#"} "&larr; Newer"]]
    [:li.next [:a {:href "#"} "Older &rarr;"]]]))

(defsection index-section [Activity :rdf]
  [activities & _]
  (vector (reduce concat (index-block activities))))

(defsection index-section [Activity :xmpp]
  [activities & options]
  ["pubsub" {} (index-block activities)])







(defsection show-section [Activity :as]
  [activity & _]
  {"published" (:published activity)
   "updated" (:updated activity)
   "verb" "post"
   "title" (:title activity)
   ;; "content" (:content activity)
   "id" (:id activity)
   "url" (full-uri activity)
   "actor" (show-section (helpers.activity/get-author activity))
   "object"
   (let [object (:object activity)]
     {"objectType" (:object-type object)
      "id" (:id object)
      "displayName" (:content activity)
      ;; "published" (:published object)
      ;; "updated" (:updated object)
      })})

(defsection show-section [Activity :atom]
  [^Activity activity & _]
  (let [entry (abdera/new-entry)]
    (doto entry
      (.setId (or (:id activity) (str (:_id activity))))
      (.setPublished (:published activity))
      (.setUpdated (:updated activity))
      (.setTitle (or (and (not= (:title activity) "")
                          (:title activity))
                     (:content activity)))
      #_(helpers.activity/add-author activity)
      (.addLink (full-uri activity) "alternate")
      (.setContentAsHtml (:content activity))
      (.addSimpleExtension
       ns/as "object-type" "activity" ns/status)
      (.addSimpleExtension
       ns/as "verb" "activity" ns/post)
      #_(helpers.activity/comment-link-item activity)
      (helpers.activity/acl-link activity))
    (let [object (:object activity)
          object-element (.addExtension entry ns/as "object" "activity")]
      #_(.setObjectType object-element ns/status)
      (if-let [object-updated (:updated object)]
        (.addSimpleExtension object-element ns/atom "updated" "" (str object-updated)))
      (if-let [object-published (:published object)]
        (.addSimpleExtension object-element ns/atom "published" "" (str object-published)))
      #_(if-let [object-id (:id object)]
          (.setId object-element object-id))
      #_(.setContentAsHtml object-element (:content activity)))
    entry))

(defsection show-section [Activity :json]
  [activity & _]
  {:text (:title activity)
   :truncated false
   :created_at (model/date->twitter (:published activity))
   :in_reply_to_status_id nil
   :source (:source activity)
   :id (:_id activity)
   :in_reply_to_user_id nil
   :in_reply_to_screen_name nil
   :favorited false
   :attachments []
   :user
   (let [user (helpers.activity/get-author activity)]
     {:name (:display-name user)
      :id (:_id user)
      :screen_name (:username user)
      :url (:url user)
      :profile_image_url (:avatar-url user)
      :protected false})
   :statusnet_html (:content activity)
   :statusnet_conversation_id nil})

(defsection show-section [Activity :html]
  [activity & _]
  (let [user (get-author activity)]
    [:article.hentry.notice
     {:id (:id activity)
      :about (uri activity)
      :typeof "sioc:Post"}
     [:header
      ;; TODO: merge into the same link
      (sections.user/display-avatar user)
      (link-to user)
      [:div.labels
       [:span.label (-> activity :object :object-type)]
       (when-not (:local activity)
         [:span.label
          [:a {:href (:id activity)} "Remote"]])
       (when-not (:public activity)
         [:span.label "Private"])]]
     (post-actions activity)
     [:div.entry-content
      #_(when (:title activity)
          [:h1.entry-title {:property "dc:title"} (:title activity)])
      [:p {:property "dc:title"}
       (or (:title activity)
           (:content activity))]]
     [:div
      (recipients-section activity)
      (links-section activity)
      (likes-section activity)
      (maps-section activity)
      (tags-section activity)
      (posted-link-section activity)
      (when (:source activity)
        (str " from " (:source activity)))
      (when (:conversation activity)
        (list
         " "
         [:a {:href (:conversation activity)}
          "in context"]))
      (when (and (seq (:lat activity))
                 (seq (:long activity)))
        (list " at "
              [:a {:href "#"}
               (:lat activity) ", "
               (:long activity)]))
      (comments-section activity)]]))

(defsection show-section [Activity :rdf]
  [activity & _]
  (with-rdf-ns ""
    (let [{:keys [content id published]} activity
          uri (full-uri activity)
          user (get-author activity)]
      (concat [
               [uri [:rdf :type]      [:sioc :Post]]
               [uri [:as  :verb]      (l "post")]
               [uri [:sioc  :content] (l content)]
               [uri [:as  :author]    (rdf-resource (or id (model.user/get-uri user)))]
               [uri [:dc  :published] (date published)]
               ]
              #_(show-section user)))))

(defsection show-section [Activity :xmpp]
  [^Activity activity & options]
  (element/abdera-to-tigase-element
   (with-format :atom
     (show-section activity))))

(defsection show-section [Activity :xml]
  [activity & _]
  [:status
   [:text (h/escape-html (or (:title activity)
                             (:content activity)))]
   [:truncated "false"]
   [:created_at (-?> activity :published model/date->twitter)]
   [:source (:source activity)]
   [:id (:_id activity)]
   [:in_reply_to_status_id]
   [:in_reply_to_user_id]
   [:favorited "false" #_(liked? (current-user) activity)]
   [:in_reply_to_screen_name]
   (show-section (get-author activity))
   [:geo]
   [:coordinates]
   [:place]
   [:contributors]
   [:entities
    [:user_mentions
     ;; TODO: list mentions
     ]
    [:urls
     ;; TODO: list urls
     ]
    [:hashtags
     ;; TODO: list hashtags
     ]]])
