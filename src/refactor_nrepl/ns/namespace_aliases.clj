(ns refactor-nrepl.ns.namespace-aliases
  (:require [refactor-nrepl.ns
             [helpers :as ns-helpers]
             [ns-parser :as ns-parser]]
            [refactor-nrepl.util :as util]))

;; The structure here is {path {lang [timestamp value]}}
;; where lang is either :clj or :cljs
(defonce ^:private cache (atom {}))

(defn- aliases [libspecs]
  (->> libspecs
       (map #(vector (:as %) (:ns %)))
       (remove #(nil? (first %)))
       distinct))

(defn- aliases-by-frequencies [libspecs]
  (->> libspecs
       (mapcat aliases) ; => [[str clojure.string] ...]
       (sort-by second)
       (group-by first) ; => {str [[str clojure.string] [str clojure.string]] ...}
       (map (comp seq frequencies second)) ; => (([[set clojure.set] 4] [set set] 1) ...)
       (map (partial sort-by second >)) ; by decreasing frequency
       (map (partial map first)) ; drop frequencies
       (map (fn [aliases] (list (ffirst aliases) (map second aliases))))
       (mapcat identity)
       (apply hash-map)))

(defn- get-cached-libspec [f lang]
  (when-let [[ts v] (get-in @cache [(.getAbsolutePath f) lang])]
    (when (= ts (.lastModified f))
      v)))

(defn- put-cached-libspec [f lang]
  (let [libspecs (ns-parser/get-libspecs-from-file
                  {:features #{lang} :read-cond :allow} f)]
    (swap! cache assoc-in [(.getAbsolutePath f) lang]
           [(.lastModified f) libspecs])
    libspecs))

(defn- get-libspec-from-file-with-caching [lang f]
  (if-let [v (get-cached-libspec f lang)]
    v
    (put-cached-libspec f lang)))

(defn namespace-aliases
  "Return a map of file type to a map of aliases to namespaces

  {:clj {util com.acme.util str clojure.string
   :cljs {gstr goog.str}}}"
  []
  {:clj (->> (util/filter-project-files (some-fn util/clj-file? util/cljc-file?))
             (map (partial get-libspec-from-file-with-caching :clj))
             aliases-by-frequencies)
   :cljs (->> (util/filter-project-files (some-fn util/cljs-file? util/cljc-file?))
              (map (partial get-libspec-from-file-with-caching :cljs))
              aliases-by-frequencies)})
