(ns leiningen.garden
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.java.io :as io]
   [leiningen.core.main :as main]
   [leiningen.core.classpath :as classpath]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project :refer [merge-profiles]]
   [leiningen.help :as help]
   [garden.core]
   [me.raynes.fs :as fs]))


(defn- builds [project]
  (-> project :garden :builds))

(defn- output-path [build]
  (-> build :compiler :output-to))

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

(defn- assert-flags [build]
  (when-not (output-path build)
    (println "No output file specified.")
    (main/abort)))

(defn- ensure-output-directory-exists [build]
  (let [dir (-> (output-path build)
                io/file
                fs/absolute-path
                fs/parent)]
    (when-not (fs/exists? dir)
      (when-not (fs/mkdirs dir)
        (println "Could not create directory" dir)
        (main/abort)))))

(defn- prepare-build [build]
  (ensure-output-directory-exists build)
  (assert-flags build))

(defn- compile-build [project build watch?]
  (prepare-build build)
  (let [stylesheet (:stylesheet build)
        flags (:compiler build)
        interval 500]
    `(if-not ~watch?
       (do (garden.core/css ~flags ~stylesheet)
           nil)
       (let [modified-namespaces# (ns-tracker/ns-tracker '~(:source-paths project))]
         (loop []
           (let [ns# (modified-namespaces#)]
             (when (seq ns#)
               (try
                 (doseq [ns-sym# ns#]
                   (require ns-sym# :reload))
                 (println "Compiling" '~stylesheet "to" ~(:output-to flags))
                 (garden.core/css ~flags ~stylesheet)
                 (println "Successful")
                 (catch Exception e#
                   (println "Error:" (.getMessage e#))))
               (flush)))
           (Thread/sleep ~interval)
           (recur))))))

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
                     '(require '[garden.core]
                               '[ns-tracker.core :as ns-tracker]))))

(defn- once
  "Compile Garden stylesheets once."
  [project args]
  (run-compiler project args false))

(defn- auto
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
  {:dependencies '[[garden "1.1.5"]
                   [ns-tracker "0.2.1"]]})

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
