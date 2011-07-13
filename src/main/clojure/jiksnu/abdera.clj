(ns jiksnu.abdera
  (:use ciste.debug
        [clojure.tools.logging :only (error)])
  (:require [clj-tigase.core :as tigase])
  (:import com.cliqset.abdera.ext.activity.ActivityExtensionFactory
           com.cliqset.abdera.ext.poco.PocoExtensionFactory
           java.io.ByteArrayInputStream
           javax.xml.namespace.QName
           org.apache.abdera.Abdera
           org.apache.abdera.factory.Factory
           org.apache.abdera.protocol.client.AbderaClient))

(defonce ^Abdera ^:dynamic *abdera* (Abdera.))
(defonce ^Factory ^:dynamic *abdera-factory* (.getFactory *abdera*))
(defonce ^:dynamic *abdera-parser* (.getParser *abdera*))
(defonce ^:dynamic *abdera-client* (AbderaClient.))

(.registerExtension *abdera-factory* (ActivityExtensionFactory.))
(.registerExtension *abdera-factory* (PocoExtensionFactory.))

(defn fetch-resource
  [uri]
  (.get (AbderaClient.) uri))

(defn fetch-document
  [uri]
  (.getDocument (fetch-resource uri)))

(defn fetch-feed
  [uri]
  (.getRoot (fetch-document uri)))

(defn fetch-entries
  [uri]
  (seq (.getEntries (fetch-feed uri))))

(defn parse-stream
  [stream]
  (try
    (let [parser *abdera-parser*]
      (.parse parser stream))
    (catch IllegalStateException e
      (error e))))

(defn parse-xml-string
  "Converts a string to an Abdera entry"
  [entry-string]
  (let [stream (ByteArrayInputStream. (.getBytes entry-string "UTF-8"))
        parsed (parse-stream stream)]
    (.getRoot parsed)))

(defn not-namespace
  "Filter for map entries that do not represent namespaces"
  [[k v]]
  (not (= k :xmlns)))

(defn make-object
  ([element]
     (com.cliqset.abdera.ext.activity.Object. element))
  ([namespace name prefix]
     (com.cliqset.abdera.ext.activity.Object.
      *abdera-factory* (QName. namespace name prefix))))

(defn find-children
  [element path]
  (if element
    (.findChild element path)))

;; FIXME: Abdera element
(defn get-qname
  "Returns a map representing the QName of the given element"
  [element]
  (tigase/parse-qname (.getQName element)))
