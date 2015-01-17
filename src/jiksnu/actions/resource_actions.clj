(ns jiksnu.actions.resource-actions
  (:use [ciste.core :only [defaction]]
        [jiksnu.actions :only [invoke-action]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [aleph.formats :refer [channel-buffer->string]]
            [aleph.http :as http]
            [ciste.config :refer [config]]
            [ciste.model :as cm]
            [clj-http.client :as client]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lamina.core :as l]
            [lamina.time :as lt]
            [lamina.trace :as trace]
            [jiksnu.channels :as ch]
            [jiksnu.model.resource :as model.resource]
            [jiksnu.ops :as ops]
            [jiksnu.session :as session]
            [jiksnu.templates.actions :as templates.actions]
            [jiksnu.transforms :as transforms]
            [jiksnu.transforms.resource-transforms :as transforms.resource]
            [jiksnu.util :as util])
  (:import jiksnu.model.Resource))

(def user-agent "Jiksnu Resource Fetcher (http://github.com/duck1123/jiksnu)")

(defonce delete-hooks (ref []))

(defn prepare-delete
  ([item]
     (prepare-delete item @delete-hooks))
  ([item hooks]
     (if (seq hooks)
       (recur ((first hooks) item) (rest hooks))
       item)))

(trace/defn-instrumented prepare-create
  [params]
  (-> params
      transforms/set-_id
      transforms/set-local
      transforms.resource/set-domain
      transforms.resource/set-location
      transforms/set-updated-time
      transforms/set-created-time
      transforms/set-no-links))

(def add-link* (templates.actions/make-add-link* model.resource/collection-name))

(defaction create
  [params]
  (let [params (prepare-create params)]
    (if-let [item (model.resource/create params)]
      item
      (throw+ "Could not create record"))))

(defaction find-or-create
  [params & [{tries :tries :or {tries 1} :as options}]]
  (if-let [item (or (model.resource/fetch-by-url (:url params))
                    (try
                      (create params)
                      (catch Exception ex)))]
    item
    (if (< tries 3)
      (do
        (log/info "recurring")
        (find-or-create params (assoc options :tries (inc tries))))
      (throw+ "Could not create resource"))))

(defaction delete
  "Delete the resource"
  [item]
  (if-let [item (prepare-delete item)]
    (do (model.resource/delete item)
        item)
    (throw+ "Could not delete resource")))

(def index*
  (templates.actions/make-indexer 'jiksnu.model.resource
                      :sort-clause {:updated -1}))

(defaction index
  [& args]
  (apply index* args))

(defn add-link
  [item link]
  (if-let [existing-link (model.resource/get-link item
                                                  (:rel link)
                                                  (:type link))]
    item
    (add-link* item link)))

(defmulti process-response-content (fn [content-type item response] content-type))

(defmethod process-response-content :default
  [content-type item response]
  (log/infof "unknown content type: %s" content-type))

(declare update)

(defn process-response
  [item response]
  (let [content-str (get-in response [:headers "content-type"])
        status (:status response)]
    (model.resource/set-field! item :status status)
    (when-let [location (get-in response [:headers "location"])]
      (let [resource (find-or-create {:url location})]
        (update resource)
        (model.resource/set-field! item :location location)))
    (let [[content-type rest] (string/split content-str #"; ?")]
      (if (seq rest)
        (let [encoding (string/replace rest "charset=" "")]
          (when (seq encoding)
            (model.resource/set-field! item :encoding encoding))))
      (model.resource/set-field! item :contentType content-type)
      (process-response-content content-type item response))))

(defn needs-update?
  [item options]
  (let [last-updated (:lastUpdated item)]
    (and (not (:local item))
         (or (:force options)
             (nil? last-updated)
             (time/after? (-> 5 time/minutes time/ago)
                          (coerce/to-date-time last-updated))))))

(defn get-body-buffer
  "Given an http response, returns a channel buffer"
  [response]
  (log/info "Getting body buffer")
  (when-let [body (:body response)]
    (let [res (l/expiring-result (lt/seconds 15))]
      (if (l/channel? body)
        (l/on-closed body
                     (fn []
                       (log/info "closed")
                       ;; (l/receive-all body println)
                       (let [buffers (l/channel->seq body #_(lt/seconds 30))]
                         (let [cb (aleph.formats/channel-buffers->channel-buffer buffers)]
                           (l/enqueue res cb)))))
        (l/enqueue res body))
      res)))

(defn decode-buffer
  [response buffer]
  (log/info "Buffer channel realized")
  (let [body-str (aleph.formats/channel-buffer->string buffer)
        response (assoc response :body body-str)]
    response))

(defn transform-response
  [response]
  ;; TODO: make this configurable
  (let [res (l/expiring-result (lt/seconds 15))]
    (l/run-pipeline
     (get-body-buffer response)
     {:error-handler (fn [ex] ex)
      :result res}
     (partial decode-buffer response))
    res))

(defn handle-unauthorized
  [item response]
  (model.resource/set-field! item :requiresAuth true)
  nil)

(defn handle-update-realized
  [item response]
  (trace/trace :resource:realized [item response])
  (model.resource/set-field! item :lastUpdated (time/now))
  (model.resource/set-field! item :status (:status response))
  (condp = (:status response)
    200 (transform-response response)
    401 (handle-unauthorized item response)
    (log/warn "Unknown status type")))

(defn update*
  "Fetches the resource and returns a result channel or nil.

The channel will receive the body of fetching this resource."
  [item & [options]]
  {:pre [(instance? Resource item)]}
  (let [url (:url item)
        actor (session/current-user)
        date (time/now)]
    (if (or true (needs-update? item options))
      (if (:requiresAuth item)
        ;; auth required
        (throw+ "Resource requires authorization")
        ;; no auth required
        (let [res (l/expiring-result (lt/seconds 30))
              auth-string (string/join
                           " "
                           ["Dialback"
                            (format "host=\"%s\"" (config :domain))
                            (format "token=\"%s\"" "4430086d")])
              request {:url url
                       :method :get
                       :headers {"User-Agent" user-agent
                                 "date" (util/date->rfc1123 (.toDate date))
                                 "authorization" auth-string}}]
          (trace/trace :resource:updated item)
          (log/infof "updating resource: %s" url)
          (l/run-pipeline
           (http/http-request request)
           {:error-handler (fn [ex]
                             ;; (log/error ex)
                             ;; (.printStackTrace ex)
                             (trace/trace :resource:failed [item ex]))
            :result res}
           (partial handle-update-realized item))
          res))
      (log/warn "Resource does not need to be updated at this time."))))

(defaction update
  [item]
  (update* item)
  item)

(defaction discover
  [item]
  (log/debugf "discovering resource: %s" (prn-str item))
  (let [response (update* item)]
    (model.resource/fetch-by-id (:_id item))))

(defaction show
  [item]
  item)
