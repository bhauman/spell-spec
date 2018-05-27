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

Only fails if there is a similar enough key to warrant failure so maps
remain open for keys that aren't similar to the specifed keys.

```clojure
(s/valid? 
  (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
  {:there 1 :hello 1 :barbara 1})
=> true
```

## Usage

## License

Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
