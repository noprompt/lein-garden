(ns leiningen.garden
  (:require [clojure.pprint :refer [pprint]]
            [leiningen.core.main :as main]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.core.project :refer [merge-profiles]]
            [garden.core]
            [me.raynes.fs :as fs]
            [clojure.core.async :refer [chan go <! put!]]))

(defn- builds [project]
  (-> project :garden :builds))

(defn- find-builds [project ids]
  (let [ids (set ids)]
    (filter #(ids (:id %)) (builds project))))

(defn- load-namespaces [syms]
  `(require
    ~@(for [sym syms]
        `'~(-> sym namespace symbol))))

(defn- locate-file [project build]
  (let [stylesheet (:stylesheet build)
        source-paths (vec (:source-paths project))]
    `(let [filename# (:file (meta #'~stylesheet))]
       (some
        (fn [path#]
          (let [file# (fs/file path# filename#)]
            (when (fs/file? file#)
              file#)))
        ~source-paths))))

(defn- compile-build [project build watch?]
  (let [stylesheet (:stylesheet build)
        flags (:compiler build)]
    `(let [file# ~(locate-file project build)]
       (if ~watch?
         (let [c# ((fn watch-file# [file#]
                     (let [mtime-chan# (chan 1)
                           mtime# (fs/mod-time file#)]
                       (go (loop [old-mtime# mtime# new-mtime# mtime#]
                             (when (not= old-mtime# new-mtime#)
                               (put! mtime-chan# new-mtime#))
                             (recur new-mtime# (fs/mod-time file#))))
                       mtime-chan#))
                   file#)]
           (go (while true
                 (when (<! c#)
                   (try
                     (load-file (str file#))
                     (garden.core/css ~flags ~stylesheet)
                     (catch Exception e#
                       (println "Error:" (.getMessage e#))))
                   (flush))))
           (put! c# 0) ; Kick off the compiler.
           (loop []
             (Thread/sleep 100)
             (recur)))
         (do
           (garden.core/css ~(:compiler build) ~(:stylesheet build))
           nil)))))

(defn- run-compiler [project args watch?]
  (let [builds (if (seq args)
                 (find-builds project args)
                 (builds project))
        requires (load-namespaces (map :stylesheet builds))]
    (println "Compiling Garden...")
    (eval-in-project project
                     `(do
                        ~requires
                        ~@(for [build builds]
                            (compile-build project build watch?)))
                     '(require 'garden.core
                               '[me.raynes.fs :as fs]
                               '[clojure.core.async :refer [go chan put! <!]]))))

(def garden-profile
  {:dependencies '[[org.clojure/clojure "1.5.1"]
                   [garden "1.1.2"]
                   [me.raynes/fs "1.4.4"]
                   [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]]})

(defn garden
  "Compile Garden stylesheets."
  [project & args]
  (let [project (merge-profiles project [garden-profile])
        [command & args] args]
    (case command
      "once" (run-compiler project args false)
      "auto" (run-compiler project args true)
      (do
        (println "Unknown command:" command)
        (main/abort)))))
