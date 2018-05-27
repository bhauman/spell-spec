# spell-spec

Provides additional spec macros that have the same signature as
`clojure.spec.alpha/keys` which check for potential spelling errors in
the map keys.

Example Specs and output:

```clojure
(explain 
  (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
  {:there 1 :helloo 1})
;; In: [:helloo 0] val: :helloo fails at: [0] predicate: (not-misspelled #{:hello :there})
;; 	 :expound.spec.problem/type  :spell-spec.core/misspelled-key
;; 	 :spell-spec.core/misspelled-key  :helloo
;; 	 :spell-spec.core/likely-misspelling-of  :hello
```

Designed to work well with expound:

```clojure
(expound 
  (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
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
  (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
  {:there 1 :hello 1 :barbara 1})
=> true
```

Also provides warnings instead of spec failures by binding
`spell-spec.core/*warn-only*` to `true`

```clojure
(binding [spell-spec.core/*warn-only* true]
  (s/valid? 
    (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
    {:there 1 :helloo 1}))
;; << printed to *err* >>
;; SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1}
=> true
```

or calling `spell-spec.core/warn-on-misspelled-keys`

```clojure
(binding [spell-spec.core/*warn-only* true]
  (s/valid? 
    (spell-spec.core/warn-on-misspelled-keys :opt-un [::hello ::there]) 
    {:there 1 :helloo 1}))
;; << printed to *err* >>
;; SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1}
=> true
```

## Usage




## License

Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
