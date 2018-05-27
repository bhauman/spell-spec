# spell-spec

Provides additional spec macros that have the same signature as
`clojure.spec.alpha/keys` which check for potential spelling errors in
the map keys. `spell-spec` provides
[expound](https://github.com/bhb/expound) integration for nicely
formatted results.

Example Specs and output:

```clojure
(explain 
  (spell-spec.core/keys :opt-un [::hello ::there]) 
  {:there 1 :helloo 1})
;; In: [:helloo 0] val: :helloo fails at: [0] predicate: (not-misspelled #{:hello :there})
;; 	 :expound.spec.problem/type  :spell-spec.core/misspelled-key
;; 	 :spell-spec.core/misspelled-key  :helloo
;; 	 :spell-spec.core/likely-misspelling-of  :hello
```

Designed to work well with [expound](https://github.com/bhb/expound):

```clojure
(expound 
  (spell-spec.core/keys :opt-un [::hello ::there]) 
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
  (spell-spec.core/keys :opt-un [::hello ::there]) 
  {:there 1 :hello 1 :barbara 1})
=> true
```

Also provides warnings instead of spec failures by binding
`spell-spec.core/*warn-only*` to `true`

```clojure
(binding [spell-spec.core/*warn-only* true]
  (s/valid? 
    (spell-spec.core/keys :opt-un [::hello ::there]) 
    {:there 1 :helloo 1}))
;; << printed to *err* >>
;; SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1}
=> true
```

or calling `spell-spec.core/warn-keys`

```clojure
(s/valid?
  (spell-spec.core/warn-keys :opt-un [::hello ::there]) 
  {:there 1 :helloo 1})
;; << printed to *err* >>
;; SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1}
=> true
```

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

## `spell-spec.core/keys`

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
