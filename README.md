# lein-garden

A Leiningen plugin for automatically compiling
[Garden](https://github.com/noprompt/garden) stylesheets.

## Requirements

This plugin requires Clojure version `1.5.1` or higher.

## Installation

Put `[lein-garden "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

```clojure
(defproject cash-money "1.1.1"
  :plugins [[lein-garden "0.1.0-SNAPSHOT"]])
```

## How do I use this?

### Preface

Let's suppose you have a project where you are currently using Garden
for CSS generation. Now, imagine you're so tired of manually
recompiling your stylesheets or resorting to some other form
skulduggery that you're thinking of going back to Sass. This causes
you to reflect deeply on your life for a moment before realizing
that it's just negativity talking and get back to hacking.

### Chapter 1

*You open `~/cash-money/src/cash-money/core.clj`.*

```clojure
(ns cash-money.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px]]))

(defstylesheet screen
  {:output-to "resources/public/screen.css"}
  [:body
   {:font-family "sans-serif"
    :font-size (px 16)
    :line-height 1.5}])
```

*There's a namespace to the north and a some Garden code to the
south.*

You think to yourself, "this is nice if you're the sort of person that
doesn't mind constantly reloading the file to recompile the CSS after
every save." Alas, you are not one of these "persons." Instead, you've
added `[lein-garden "X.X.X"]` to your `:plugins` vector, read
*"Getting Things Done"*, and like Landon Austin are ready for
anything.

### Chapter 2

*You open `~/cash-money/project.clj`*

```clojure
(defproject cash-money "1.1.1"
  :plugins [[lein-garden "0.1.0-SNAPSHOT"]])
```

To get everything going with `lein garden` you add the remaining
ingredients.

```clojure
(defproject cash-money "1.1.1"
  :plugins [[lein-garden "0.1.0-SNAPSHOT"]]
  :garden {:builds [{;; Optional name of the build.
                     :id "screen"
                     ;; The var containing your stylesheet.
                     :stylesheet cash-money.core/screen
                     ;; Compiler flags passed to `garden.core/css`
                     :compiler {;; Where to save the file.
                                :output-to "resources/screen.css"
                                ;; Compress the output?
                                :pretty-print? false}}]})
```

Next, you open `~/cash-money/src/cash-money/core.clj` and make the
make a small change.

```clojure
(ns cash-money.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px]]))

(defstyles screen
  [:body
   {:font-family "sans-serif"
    :font-size (px 16)
    :line-height 1.5}])
```

Finally you run

```shell
$ lein garden auto
```

and behold as your stylesheet is automatically compiled.

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
