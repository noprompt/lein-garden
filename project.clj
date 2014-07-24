(defproject lein-garden "0.1.10-SNAPSHOT"
  :description "A Leiningen plugin for automatically compiling Garden stylesheets"
  :url "https://github.com/noprompt/lein-garden"
  :license {:name "Unlicense"
            :url "http://unlicense.org/UNLICENSE"}
  :eval-in-leiningen true
  :dependencies [[garden "1.1.5"]
                 [me.raynes/fs "1.4.4"]
                 [ns-tracker "0.2.1"]])
