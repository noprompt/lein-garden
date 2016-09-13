(defproject lein-garden "0.3.1-SNAPSHOT"
  :description "A Leiningen plugin for automatically compiling Garden stylesheets"
  :url "https://github.com/noprompt/lein-garden"
  :license {:name "Unlicense"
            :url "http://unlicense.org/UNLICENSE"}
  :min-lein-version "2.5.0"
  :eval-in-leiningen true
  :dependencies [[garden "1.3.1"]
                 [me.raynes/fs "1.4.6"]
                 [ns-tracker "0.3.0"]])
