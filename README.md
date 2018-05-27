# spell-spec

Provides additional spec macros that have the same signature as
`clojure.spec.alpha/keys` which check for potential spelling errors in
the map keys. `spell-spec` provides
[expound](https://github.com/bhb/expound) integration for nicely
formatted results.

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

Maps remain open for keys that aren't similar to the specifed keys.

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
misstyped map keys. This is true for tool configuration and possibly
any external API where users are repeatedly stung my single character
misshaps. `spell-spec` can provide valuable feedback for these
situations.

The behavior in the library is an evolution of the library
[strictly-specking](https://github.com/bhauman/strictly-specking),
which I wrote to validate the complex configuration of
[figwheel](https://github.com/bhauman/lein-figwheel).

When I originally wrote
[strictly-specking](https://github.com/bhauman/strictly-specking), I
really wanted to push the limits of what could be done to provide
feedback for configuration errors. As a result the code in is very
complex and tailored to the problem domain of configuration
specification for a tool like [figwheel](https://github.com/bhauman/lein-figwheel).

`spell-spec` is a *good enough* approach which will provide good
feedback for a much broader set of use cases. I am planning on using
this approach instead of
[strictly-specking](https://github.com/bhauman/strictly-specking) from
now on.

`spell-spec` is much much lighter as it has no dependencies other than
of `clojure.spec` itself.

## Usage

Add `spell-spec` as a dependency in your project config.

For *leiningen* in your `project.clj` `:dependencies` add:

```clojure
:dependencies [[spell-spec "0.1.0"]
               ;; if you want to use expound
               [expound "0.6.1"]]
```

For *clojure cli tools* in your `deps.edn` `:deps` key add:

```clojure
{:deps {spell-spec {:mvn/version "0.1.0"}
        ;; if you want to use expound
        expound {:mvn/version "0.6.1"}}}
```

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
*misspelled keys* and one for *unknown keys*.

> I really debated about whether I should add `strict-keys` to the
> library as it violates the Clojure idiom of keeping maps
> open. However, there are some situations where this behavior is
> warranted. I stronly advocate for the use of `spell-spec.alpha/keys`
> over `strict-keys`. Don't say I didn't warn you.

Example (continuation of the example session above):

```
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



## License

Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
