(ns cljspazzer.http.cache
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [cljspazzer.utils :as utils]
            [pantomime.mime :refer [mime-type-of]]))

(defn make-key [& keys]
  (let [result (utils/chk-sum-str (interpose ":" keys))]
    (prn {:result result :keys keys})
    result))

(defn cache-root []
  (let [result (io/file ".images")]
    (if (or (and (.exists result) (.isDirectory result)) (.mkdir result))
      result
      nil)))

(defn cache-response
  ([url the-cache-root keep? & name]
   (prn (format "caching %s for %s" url name))
   (let [res (client/get url {:as :byte-array
                              :throw-exceptions false})
         status (:status res)
         content-type ((:headers res) "Content-Type")
         body (:body res)
         cache-path (.getAbsolutePath the-cache-root)
         k (apply make-key (if (seq? name) name [name]))
         extension (utils/get-extension-for-mime (or content-type (mime-type-of body) ".jpg"))
         file-name (format "%s/%s%s" cache-path k extension)]
     (if (and (= 200 status) (keep? res))
       (with-open [w (io/output-stream file-name)]
         (prn (format "saving %s" file-name))
         (.write w body)
         (io/file file-name))
       nil)))
  ([url name]
   (cache-response url (cache-root) (fn [r] true) name)))

(defn image-response? [r]
  (utils/starts-with? (or ((:headers r) "Content-type")
                          (mime-type-of (:body r)))
                      "image"))

(defn cache-image-response [url & k]
  (apply cache-response url (cache-root) image-response? k))
