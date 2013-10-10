(ns leiningen.garden
  (:require [clojure.pprint :refer [pprint]]
            [leiningen.core.main :as main]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.core.project :refer [merge-profiles]]
            [leiningen.help :as help]
            [garden.core]
            [me.raynes.fs :as fs]
            [clojure.core.async :refer [chan go <! put!]]))

(defn- builds [project]
  (-> project :garden :builds))

(defn- find-builds [project ids]
  (let [id? (set ids)]
    (for [{:keys [id] :as build} (builds project)]
      (if (id? id)
        build
        (throw (Exception. (str "Unknown build identifier: " id)))))))

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

(defn once
  "Compile Garden stylesheets once."
  [project args]
  (run-compiler project args false))

(defn auto
  "Automatically recompile when files are modified."
  [project args]
  (run-compiler project args true))

(defn- validate-builds [project]
  (doseq [{:keys [stylesheet]} (builds project)]
    (cond
     (nil? stylesheet)
     (throw (Exception. "No stylesheet specified."))
     (not (symbol? stylesheet))
     (throw (Exception. "Stylesheet must be a symbol")))))

(def ^:private garden-profile
  {:dependencies '[[org.clojure/clojure "1.5.1"]
                   [garden "1.1.2"]
                   [me.raynes/fs "1.4.4"]
                   [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]]})

(defn garden
  "Compile Garden stylesheets."
  {:help-arglists '([once auto])
   :subtasks [#'once #'auto]}
  [project & args]
  (let [project (merge-profiles project [garden-profile])
        [command & args] args]
    (validate-builds project)
    (case command
      "once" (once project args)
      "auto" (auto project args)
      (do
        (println
         (when command (str "Unknown command:" command))
         (help/subtask-help-for *ns* #'garden))
        (main/abort)))))
