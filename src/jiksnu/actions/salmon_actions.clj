(ns jiksnu.actions.salmon-actions
  (:use (ciste [config :only [definitializer]]
               [core :only [defaction]]
               [debug :only [spy]])
        (clojure.core [incubator :only [-?>]]))
  (:require (jiksnu [abdera :as abdera])
            (jiksnu.actions [activity-actions :as actions.activity])
            (jiksnu.helpers [activity-helpers :as helpers.activity])
            (jiksnu.model [user :as model.user]
                          [signature :as model.signature])
            [saxon :as s])
  (:import jiksnu.model.User
           org.apache.commons.codec.binary.Base64))

(defn get-key
  [^User author]
  (-?> author
       model.signature/get-key-for-user
       model.signature/get-key-from-armored))

(defn signature-valid?
  [envelope key]
  #_(->> (.verify default-sig envelope (list key))
         .getSignatureVerificationResults
         (map #(.isVerified %))
         (some identity)))

(defn decode-envelope
  [envelope]
  (let [data (:data envelope)]
    (String. (Base64/decodeBase64 data))))

(defn extract-activity
  [envelope]
  (-?> envelope
       decode-envelope
       abdera/parse-xml-string
       actions.activity/entry->activity))

(defn stream->envelope
  "convert an input stream to an envelope"
  [input-stream]
  (let [doc (s/compile-xml input-stream)]
    {:sig (str (s/query "//*[local-name()='sig']/text()" doc))
     :data (str (s/query "//*[local-name()='data']/text()" doc))}))

(defaction process
  [stream]
  (let [envelope (stream->envelope stream)]
    (if-let [activity (extract-activity envelope)]
      (if-let [key (-?> activity
                        helpers.activity/get-author
                        get-key)]
        (if (signature-valid? envelope key)
          (actions.activity/remote-create activity))))))

(definitializer
  (doseq [namespace ['jiksnu.filters.salmon-filters
                     'jiksnu.views.salmon-views]]
    (require namespace)))
