# lein-garden

A Leiningen plugin for automatically compiling
[Garden](https://github.com/noprompt/garden) stylesheets.

## Requirements

This plugin requires Clojure version `1.6.0` or higher and Leiningen 
version `2.4.3` or higher.

## Installation

Put `[lein-garden "0.2.0"]` into the `:plugins` vector of your project.clj.

## How do I use this?

### Preface

Let's suppose you have a project where you are currently using Garden
for CSS generation. Now, imagine you're so tired of manually
recompiling your stylesheets or resorting to some other form
of skulduggery that you're thinking of going back to Sass. This causes
you to reflect deeply on your life for a moment before realizing
that it's just negativity talking and get back to hacking.

### Chapter 1

*You open `~/cash-money/src/cash_money/core.clj`.*

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
  :plugins [[lein-garden "X.X.X"]])
```

To get everything going with `lein garden` you add the remaining
ingredients.

```clojure
(defproject cash-money "1.1.1"
  :plugins [[lein-garden "X.X.X"]]
  :garden {:builds [{;; Optional name of the build:
                     :id "screen"
                     ;; Source paths where the stylesheet source code is
                     :source-paths ["src/styles"]
                     ;; The var containing your stylesheet:
                     :stylesheet cash-money.core/screen
                     ;; Compiler flags passed to `garden.core/css`:
                     :compiler {;; Where to save the file:
                                :output-to "resources/screen.css"
                                ;; Compress the output?
                                :pretty-print? false}}]})
```

Next, you open `~/cash-money/src/cash_money/core.clj` and make a small change.

```clojure
(ns cash-money.core
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px]]))

;; Change defstylesheet to defstyles.
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

and behold as your stylesheet is automatically recompiled on save.

### Chapter 3

Now you might want stylesheets to always compile whenever starting your program with leiningen.
Add this to your `project.clj`

```:hooks [leiningen.garden]```

You might not want your stylesheets to compile before test-runs.
Perhaps what you really wanted, is to compile stylesheets before creating a jar.
You can use profiles to do this.

```:profiles {:uberjar {:hooks [leiningen.garden]}}```
