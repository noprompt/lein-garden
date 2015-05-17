(defproject lein-garden "0.2.7-SNAPSHOT"
  :description "A Leiningen plugin for automatically compiling Garden stylesheets"
  :url "https://github.com/noprompt/lein-garden"
  :license {:name "Unlicense"
            :url "http://unlicense.org/UNLICENSE"}
  :min-lein-version "2.5.0"
  :eval-in-leiningen true
  :dependencies [[garden "1.2.1"]
                 [me.raynes/fs "1.4.4"]
                 [ns-tracker "0.2.2"]])
