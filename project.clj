(defproject lein-garden "0.1.2-SNAPSHOT"
  :description "A Leiningen plugin for automatically compiling Garden stylesheets"
  :url "https://github.com/noprompt/lein-garden"
  :license {:name "Unlicense"
            :url "http://unlicense.org/UNLICENSE"}
  :eval-in-leiningen true
  :dependencies [[garden "1.1.2"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]])
