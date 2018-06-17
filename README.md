# spell-spec

[![Clojars Project](https://img.shields.io/clojars/v/com.bhauman/spell-spec.svg)](https://clojars.org/com.bhauman/spell-spec)

<img src="https://s3.amazonaws.com/bhauman-blog-images/misspelled-key-error.png"/>

<img src="https://s3.amazonaws.com/bhauman-blog-images/unknown-key-error.png"/>

`spell-spec` is a Clojure/Script library that provides additional spec
macros that have the same signature as `clojure.spec.alpha/keys`
macro. `spell-spec` macros will also verify that unspecified map keys are
not misspellings of specified map keys. `spell-spec` also provides
[expound](https://github.com/bhb/expound) integration for nicely
formatted results.

If you are unfamiliar with
[Clojure Spec](https://clojure.org/guides/spec) you can learn more
from the official [guide to Clojure Spec](https://clojure.org/guides/spec).

Example Specs and output:

```clojure
(explain 
  (spell-spec.alpha/keys :opt-un [::hello ::there]) 
  {:there 1 :helloo 1})
;; In: [:helloo 0] val: :helloo fails at: [0] predicate: (not-misspelled #{:hello :there})
;; 	 :expound.spec.problem/type  :spell-spec.alpha/misspelled-key
;; 	 :spell-spec.alpha/misspelled-key  :helloo
;; 	 :spell-spec.alpha/likely-misspelling-of  :hello
```

Designed to work well with [expound](https://github.com/bhb/expound):

```clojure
(expound 
  (spell-spec.alpha/keys :opt-un [::hello ::there]) 
  {:there 1 :helloo 1})
;; -- Misspelled map key -------------
;;
;;   {:there ..., :helloo ...}
;;                ^^^^^^^
;;
;; should be spelled
;;
;;   :hello
;;
;; -------------------------
;; Detected 1 error
```

Maps remain open for keys that aren't similar to the specified keys.

```clojure
(s/valid? 
  (spell-spec.alpha/keys :opt-un [::hello ::there]) 
  {:there 1 :hello 1 :barbara 1})
=> true
```

Also provides warnings instead of spec failures by binding
`spell-spec.alpha/*warn-only*` to `true`

```clojure
(binding [spell-spec.alpha/*warn-only* true]
  (s/valid? 
    (spell-spec.alpha/keys :opt-un [::hello ::there]) 
    {:there 1 :helloo 1}))
;; << printed to *err* >>
;; SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1}
=> true
```

or calling `spell-spec.alpha/warn-keys`

```clojure
(s/valid?
  (spell-spec.alpha/warn-keys :opt-un [::hello ::there]) 
  {:there 1 :helloo 1})
;; << printed to *err* >>
;; SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1}
=> true
```

## Why?

In certain situations there is a need to provide user feedback for
miss-typed map keys. This is true for tool configuration and possibly
any external API where users are repeatedly stung by single character
mishaps. `spell-spec` can provide valuable feedback for these
situations.

This library is an evolution of the library
[strictly-specking](https://github.com/bhauman/strictly-specking),
which I wrote to validate the complex configuration of
[figwheel](https://github.com/bhauman/lein-figwheel).

When I originally wrote
[strictly-specking](https://github.com/bhauman/strictly-specking), I
really wanted to push the limits of what could be done to provide
feedback for configuration errors. As a result the code in
`strictly-specking` is very complex and tailored to the problem domain
of configuration specification for a tool like
[figwheel](https://github.com/bhauman/lein-figwheel).

When used with expound, `spell-spec` is a **good enough** approach
which will provide good feedback for a much broader set of use
cases. I am planning on using this approach instead of
[strictly-specking](https://github.com/bhauman/strictly-specking) from
now on.

`spell-spec` is much lighter as it has no dependencies other than
of `clojure.spec` itself.

## Usage

Add `spell-spec` as a dependency in your project config.

For **leiningen** in your `project.clj` `:dependencies` add:

```clojure
:dependencies [[com.bhauman/spell-spec "0.1.0"]
               ;; if you want to use expound
               [expound "0.7.0"]]
```

For **clojure cli tools** in your `deps.edn` `:deps` key add:

```clojure
{:deps {com.bhauman/spell-spec {:mvn/version "0.1.0"}
        ;; if you want to use expound
        expound {:mvn/version "0.7.0"}}}
```

## Using with Expound

`spell-spec` does not declare `expound` as a dependency and does not
automatically register its expound helpers.

If you want to use the `spell-spec`
[expound](https://github.com/bhb/expound) integration, then after
`expound.alpha` has been required you will need to require
`spell-spec.expound` to register the expound helpers. You will want to
do this before you validate any `spell-spec` defined specs.

### `spell-spec.alpha/keys`

`keys` is likely the macro that you will use most often when using
`spell-spec`.

Use `spell-spec.alpha/keys` the same way that you would use
`clojure.spec.alpha/keys` keeping in mind that the spec it creates
will fail for keys that are misspelled.

`spell-spec.alpha/keys` is a spec macro that has the same signature
and behavior as `clojure.spec.alpha/keys`. In addition to performing
the same checks that `clojure.spec.alpha/keys` does, it checks to see
if there are unknown keys present which are also close misspellings of
the specified keys.

An important aspect of this behavior is that the map is left open to
other keys that are not close misspellings of the specified
keys. Keeping maps open is an important pattern in Clojure which
allows one to simply add behavior to a program by adding extra data to
maps that flow through functions. `spell-spec.alpha/keys` keeps this
in mind and is fairly conservative in its spelling checks.

An example of using:

```clojure
(require '[clojure.spec.alpha :as s])
(require '[spell-spec.alpha :as spell])

(s/def ::name string?)
(s/def ::use-history boolean?)

(s/def ::config (spell/keys :opt-un [::name ::use-history]))

(s/valid? ::config {:name "John" :use-hisory false :countr 1})
;; => false

(s/explain ::config {:name "John" :use-hisory false :countr 1})
;; In: [:use-hisory 0] val: :use-hisory fails at: [0] predicate: (not-misspelled #{:name :use-history})
;; 	 :expound.spec.problem/type  :spell-spec.alpha/misspelled-key
;; 	 :spell-spec.alpha/misspelled-key  :use-hisory
;; 	 :spell-spec.alpha/likely-misspelling-of  :use-history

;; to use with expound must first require expound
(require '[expound.alpha :refer [expound]])

;; and then the optional spell-spec expound helpers
(require 'spell-spec.expound)

(expound ::config {:name "John" :use-hisory false :countr 1})
;; -- Misspelled map key -------------
;;
;;   {:name ..., :use-hisory ..., :counter ...}
;;               ^^^^^^^^^^^
;;
;; should be spelled
;;
;;   :use-history
;;
;; -------------------------
;; Detected 1 error
```

### `spell-spec.alpha/strict-keys`

`strict-keys` is very similar to `spell-spec.alpha/keys` except that
the map is closed to keys that are not specified.

`strict-keys` will produce two types of validation problems: one for
**misspelled keys** and one for **unknown keys**.

> I really debated about whether I should add `strict-keys` to the
> library as it violates the Clojure idiom of keeping maps
> open. However, there are some situations where this behavior is
> warranted. I strongly advocate for the use of `spell-spec.alpha/keys`
> over `strict-keys`  ... don't say I didn't warn you.

Example (continuation of the example session above):

```clojure
(s/def ::strict-config (spell/strict-keys :opt-un [::name ::use-history]))

(s/valid? ::strict-config {:name "John" :use-hisory false :countr 1})
;; => false

(s/explain ::strict-config {:name "John" :use-hisory false :countr 1})
;; In: [:use-hisory 0] val: :use-hisory fails at: [0] predicate: #{:name :use-history}
;;   :expound.spec.problem/type  :spell-spec.alpha/misspelled-key
;; 	 :spell-spec.alpha/misspelled-key  :use-hisory
;; 	 :spell-spec.alpha/likely-misspelling-of  :use-history
;; In: [:countr 0] val: :countr fails at: [0] predicate: #{:name :use-history}
;; 	 :expound.spec.problem/type  :spell-spec.alpha/unknown-key
;;	 :spell-spec.alpha/unknown-key  :countr

(s/expound ::strict-config {:name "John" :use-hisory false :countr 1})
;; -- Misspelled map key -------------
;;
;;   {:name ..., :countr ..., :use-hisory ...}
;;                            ^^^^^^^^^^^
;;
;; should be spelled
;;
;;   :use-history
;;
;; -- Unknown map key ----------------
;;
;;   {:name ..., :use-hisory ..., :countr ...}
;;                                ^^^^^^^
;;
;; should be one of
;;
;;   :name, :use-history
;;
;; -------------------------
;; Detected 2 errors
```

## Warnings only

One way to keep maps completely open is to simply warn when keys are
misspelled or unknown, helpful feedback is still provided but the spec
doesn't fail when these anomalies are detected.

Specs defined by `spell-spec.alpha/keys` and `spell-spec.alpha/strict-keys`
will issue warnings instead of failing when one binds
`spell-spec.alpha/*warn-only*` to `true` around the calls that verify
the specs.

One can also use the following substitutions to get warnings instead of failures:

* use `spell-spec.alpha/warn-keys` for `spell-spec.alpha/keys`
* use `spell-spec.alpha/warn-strict-keys` for `spell-spec.alpha/strict-keys`

## Handling warnings

By default warnings are printed to `clojure.core/*err*`. One can
control how `spell-spec` warnings are reported by binding
`spell-spec.alpha/*warning-handler*` to a function of one argument.

Example (continuing):

```clojure
(s/def ::warn-config (spell/warn-strict-keys :opt-un [::name ::use-history]))

(binding [spell/*warning-handler* clojure.pprint/pprint]
  (s/valid? ::warn-config {:name "John" :use-hisory false :countr 1}))
;; << prints out >>
;; {:path [0],
;;  :pred #{:name :use-history},
;;  :val :use-hisory,
;;  :via [],
;;  :in [:use-hisory 0],
;;  :expound.spec.problem/type :spell-spec.alpha/misspelled-key,
;;  :spell-spec.alpha/misspelled-key :use-hisory,
;;  :spell-spec.alpha/likely-misspelling-of :use-history,
;;  :spell-spec.alpha/warning-message
;;  "possible misspelled map key :use-hisory should probably be :use-history in {:name \"John\", :use-hisory false, :countr 1}"
;;  :spell-spec.alpha/value {:name "John", :use-hisory false, :countr 1}}
;; {:path [0],
;;  :pred #{:name :use-history},
;;  :val :countr,
;;  :via [],
;;  :in [:countr 0],
;;  :expound.spec.problem/type :spell-spec.alpha/unknown-key,
;;  :spell-spec.alpha/unknown-key :countr,
;;  :spell-spec.alpha/warning-message
;;  "unknown map key :countr in {:name \"John\", :use-hisory false, :countr 1}"
;;  :spell-spec.alpha/value {:name "John", :use-hisory false, :countr 1}}
;; => true
```

## Changing the threshold that detects misspelling 

A misspelling is detected when an unknown map key is within a certain
`levenshtein` distance from a specified map key. If the size of this
distance is too big then the number of false positives goes up.

You can override the default behavior by binding the
`spell-spec.alpha/*length->threshold*` to a function that takes one
argument, the length of the shortest keyword (of two compared
keywords) and returns an integer which is the threshold for the
levenshtein distance.

Example (continuing):

```clojure
(s/def ::namer (spell/keys :opt-un [::name]))

;; :namee one character off from :name an thus a detected misspelling
;; with a threshold of 1
(binding [spell/*length->threshold* (fn [_] 1)]
  (s/valid? ::namer {:namee "John"}))
;; => false

;; :nameee is two characters off from :name an thus an un-detected misspelling
;; with a threshold of 1
(binding [spell/*length->threshold* (fn [_] 1)]
  (s/valid? ::namer {:nameee "John"})) 
;; => true

;; with a threshold of 2 we can detect both of the above misspellings
(binding [spell/*length->threshold* (fn [_] 2)]
  (s/valid? ::namer {:namee "John"}))
;; => false
(binding [spell/*length->threshold* (fn [_] 2)]
  (s/valid? ::namer {:nameee "John"})) 
;; => false
```

## License

Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
