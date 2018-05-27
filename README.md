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
misstyping map keys. This is true for tool configuration and possibly
any external API where users are repeatedly stung my single character
misshaps. This library can provide in-valuable feedback for these
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

## `spell-spec.alpha/keys`

`keys` is the likely that macro that you will use the
most when using `spell-spec`.

`keys` is a spec macro that has the same signature and behavior as
`clojure.spec.alpha/keys`. In addition to performing the same checks
that `clojure.spec.alpha/keys` does, it checks to see if there are
unknown keys present which are also close misspellings of the
specified keys.

An important aspect of this behavior is that the map is left open to
other keys that are not close misspellings of the specified keys.



## License

Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
