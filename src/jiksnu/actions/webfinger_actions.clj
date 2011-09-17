(ns jiksnu.actions.webfinger-actions
  (:use (ciste core debug)
        (jiksnu model namespace))
  (:require (jiksnu.actions [domain-actions :as actions.domain]
                            [user-actions :as actions.user])
            (jiksnu.model [user :as model.user]))
  (:import com.cliqset.hostmeta.JavaNetXRDFetcher
           com.cliqset.hostmeta.HostMeta
           com.cliqset.magicsig.keyfinder.MagicPKIKeyFinder
           com.cliqset.xrd.XRD
           java.net.URI
           java.net.URL
           org.openxrd.xrd.core.impl.XRDBuilder))

(defonce ^:dynamic *fetcher*
  (JavaNetXRDFetcher.))

(defonce ^:dynamic *xrd-builder*
  (XRDBuilder.))

(defn fetch
  "returns a cliqset xrd corresponding to the given url"
  [url]
  (if url (.fetchXRD *fetcher* (URL. url))))

(defaction host-meta
  []
  (let [xrd (XRD.)]
    ;; TODO: add the other info items
    (.buildObject *xrd-builder*)))

(defaction user-meta
  [uri]
  (->> uri
       model.user/split-uri
       (apply model.user/show )))

(defn parse-link
  "Turns a XRD link into a may with the same info"
  [link]
  (let [href (str (.getHref link))
        rel (str (.getRel link))
        template (.getTemplate link)
        type (.getType link)]
    (merge {}
           (if href {:href href})
           (if rel {:rel rel})
           (if template {:template template})
           (if type {:type type}))))

(defn get-user-meta-uri
  [user]
  (let [username (:username user)
        domain (model.user/get-domain user)]
    (or (:user-meta-uri user)
        (actions.domain/get-user-meta-uri domain username)))
  )

(defn fetch-user-meta
  [^User user]
  (-> user
      model.user/user-meta-uri
      actions.webfinger/fetch))



(defn get-links
  [^XRD xrd]
  (map parse-link (.getLinks xrd)))

(defn get-keys
  [uri]
  (let [host-meta (HostMeta/getDefault)
        key-finder (MagicPKIKeyFinder. host-meta)]
    (seq (.findKeys key-finder (URI. (spy uri))))))

;; TODO: Collect all changes and update the user once.
(defaction update-usermeta
  [user]
  (let [xrd (fetch-user-meta user)
        links (get-links xrd)
        new-user (assoc user :links links)
        feed (fetch-user-feed new-user)
        author (.getAuthor feed)
        uri (.getUri author)]
    (doseq [link links]
      ;; TODO: process this in a trigger
      (if (= (:rel link) "magic-public-key")
        (let [key-string (:href link)
              [_ n e]
              (re-matches
               #"data:application/magic-public-key,RSA.(.+)\.(.+)"
               key-string)]
          (model.signature/set-armored-key (:_id user) n e)))
      (actions.user/add-link user link))
    (-> user
        (assoc :remote-id (str uri))
        (assoc :discovered true)
        actions.user/update)))




