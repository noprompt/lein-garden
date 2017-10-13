(ns leiningen.garden
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.java.io :as io]
   [leiningen.core.main :as main]
   [leiningen.core.classpath :as classpath]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project :refer [merge-profiles]]
   [leiningen.help :as help]
   [me.raynes.fs :as fs]))

(defn- builds [project]
  (-> project :garden :builds))

(defn- output-path [build]
  (-> build :compiler :output-to))

(defn- find-builds [project ids]
  (let [all-builds (builds project)]
    (for [id ids]
      (if-let [build (first (filter #(= (:id %) id) all-builds))]
        build
        (throw (Exception. (str "Unknown build identifier: " id)))))))

(defn- validate-builds [project]
  (doseq [{:keys [id stylesheet source-paths] :as build} (builds project)]
    (cond
     (nil? source-paths)
     (throw (Exception. (format "No source-paths specified in build %s. " (name id))))
     (nil? stylesheet)
     (throw (Exception. (format "No stylesheet specified in build %s. " (name id))))
     (not (symbol? stylesheet))
     (throw (Exception. (format ":stylesheet value must be a symbol in build %s." (name id))))
     (nil? (output-path build))
     (throw (Exception. (format "No :output-to file specified in build %s." (name id)))))))

;; I'm unsure if this is actually necessary.
(defn- load-namespaces [syms]
  `(require
    ~@(for [sym syms]
        `'~(-> sym namespace symbol))))

(defn- ensure-output-directory-exists [build]
  (let [dir (-> (output-path build)
                io/file
                fs/absolute
                fs/parent)]
    (when-not (fs/exists? dir)
      (when-not (fs/mkdirs dir)
        (println "Could not create directory" dir)
        (main/abort)))))

(defn- prepare-build [build]
  (ensure-output-directory-exists build))

(defn- compile-builds [project builds watch?]
  (let [interval 500
        ;; Initial list of namespaces.
        nss (map (comp symbol namespace :stylesheet) builds)
        builds (map
                (fn [build]
                  (let [s (:stylesheet build)]
                    (assoc build :stylesheet `(var ~s))))
                builds)]
    `(let [modified-namespaces# (ns-tracker/ns-tracker '~(:source-paths project))
           builds# (list ~@builds)]
       (loop [nss# '~nss]
         (when (seq nss#)
           (doseq [build# builds#]
             (try
               (doseq [ns-sym# nss#] (require ns-sym# :reload))
               (let [stylesheet# (deref (:stylesheet build#))
                     flags# (:compiler build#)]
                 (println (str "Compiling " (pr-str (:output-to flags#)) "..."))
                 (garden.core/css flags# stylesheet#)
                 (println "Successful"))
               (catch Exception e#
                 (println "Error:" (.getMessage e#)))))
           (flush))
         (when ~watch?
           (Thread/sleep ~interval)
           (recur (modified-namespaces#)))))))

(defn- run-compiler [project args watch?]
  (let [builds (if (seq args)
                 (find-builds project args)
                 (builds project))
        build-paths (mapcat :source-paths builds)
        modified-project (-> project
                             (select-keys [:dependencies
                                           :plugin
                                           :local-repo
                                           :garden
                                           :repositories
                                           :mirrors])
                             (update-in [:source-paths] concat build-paths))
        requires (load-namespaces (map :stylesheet builds))]
    (when (seq builds)
      (doseq [build builds] (prepare-build build))
      (println "Compiling Garden...")
      (eval-in-project modified-project
                       `(do
                          ~requires
                          ~(compile-builds modified-project builds watch?))
                       '(require '[garden.core]
                                 '[ns-tracker.core :as ns-tracker])))))

(defn- once
  "Compile Garden stylesheets once."
  [project args]
  (run-compiler project args false))

(defn- auto
  "Automatically recompile when files are modified."
  [project args]
  (run-compiler project args true))


(def ^:private garden-profile
  {:dependencies '[^:displace [garden "1.2.1"]
                   ^:displace [ns-tracker "0.2.2"]]})

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
