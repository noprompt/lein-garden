(ns leiningen.garden
  (:require [clojure.pprint :refer [pprint]]
            [leiningen.core.main :as main]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.eval :refer [eval-in-project]]))

(defn load-namespaces [syms]
  `(require
    ~@(for [sym syms]
        `'~(-> sym namespace symbol))))

(defn builds [project]
  (-> project :garden :builds))

(defn find-builds [project ids]
  (let [ids (set ids)]
    (filter #(ids (:id %)) (builds project))))

(defn compile-builds [builds]
  (let [stylesheets (map :stylesheet builds)
        compiler-flags (map :compiler builds)
        requires (load-namespaces stylesheets)
        gardens (->> (interleave stylesheets compiler-flags)
                     (partition 2))]
    `(do
       ~requires
       ~@(for [[stylesheet flags] gardens]
           `(garden.core/css ~flags ~stylesheet)))))

(defn once [project args]
  (let [builds (if (seq args)
                 (find-builds project args)
                 (builds project))]
    (eval-in-project project (compile-builds builds))))

(defn garden
  "Compile Garden stylesheets."
  [project & args]
  (let [[command & args] args]
    (case command
      "once" (once project args)
      (do
        (println "Unknown command:" command)
        (main/abort)))))
