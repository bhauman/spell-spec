(ns spell-spec.alpha
  (:refer-clojure :exclude [keys])
  (:require
   [#?(:clj clojure.spec.alpha
       :cljs cljs.spec.alpha)
    :as s])
  #?(:cljs (:require-macros [spell-spec.alpha :refer [keys warn-keys strict-keys warn-strict-keys]])))

(def ^:dynamic *value* {})

(def ^:dynamic *warn-only* false)

(def default-warning-handler #(some->> % ::warning-message
                                       (str "SPEC WARNING: ")
                                       println))

(def ^:dynamic *warning-handler* default-warning-handler)

;; this is a simple step function to determine the threshold
;; no need to figure out the numeric function
(defn length->threshold [len]
  (condp #(<= %2 %1) len
    4 0
    5 1
    6 2
    11 3
    20 4
    (int (* 0.2 len))))

(def ^:dynamic *length->threshold* length->threshold)

;; ----------------------------------------------------------------------
;; similar keywords

(defn- next-row
  [previous current other-seq]
  (reduce
    (fn [row [diagonal above other]]
      (let [update-val (if (= other current)
                          diagonal
                          (inc (min diagonal above (peek row))))]
        (conj row update-val)))
    [(inc (first previous))]
    (map vector previous (next previous) other-seq)))

(defn- levenshtein
  "Compute the levenshtein distance between two [sequences]."
  [sequence1 sequence2]
  (peek
    (reduce (fn [previous current] (next-row previous current sequence2))
            (map #(identity %2) (cons nil sequence2) (range))
            sequence1)))

(defn- similar-key* [thresh ky ky2]
  (let [dist (levenshtein (str ky) (str ky2))]
    (when (<= dist thresh)
      dist)))

(defn- similar-key [ky ky2]
  (let [min-len (apply min (map (comp count #(if (.startsWith % ":") (subs % 1) %) str) [ky ky2]))]
    (similar-key* (#?(:clj *length->threshold*
                      :cljs length->threshold)
                   min-len) ky ky2)))

(defn likely-misspelled [known-keys]
  (fn [key]
    (and (not (known-keys key))
         (->> known-keys
              (filter #(similar-key % key))
              (remove (set (#?(:clj clojure.core/keys
                               :cljs cljs.core/keys)
                            *value*)))
              not-empty))))

(defn not-misspelled [known-keys] (complement (likely-misspelled known-keys)))

(defn- most-similar-to [key known-keys]
  (->> ((likely-misspelled known-keys) key)
       (map (juxt #(levenshtein (str %) (str key)) identity))
       (filter first)
       (sort-by first)
       first
       second))

(defn- enhance-spelling-problem [known-keys {:keys [val] :as prob}]
  (if-let [sim (most-similar-to val known-keys)]
    (assoc prob
           :expound.spec.problem/type ::misspelled-key
           ::misspelled-key val
           ::likely-misspelling-of sim)
    (assoc prob
           :expound.spec.problem/type ::unknown-key
           ::unknown-key val)))

(defmulti warning-message* (fn [a _] (:expound.spec.problem/type a)))

(defmethod warning-message* :default [{:keys [val pred]} value]
  (str "Value " (pr-str val) " failed predicate " (pr-str pred) " in "
       (binding [*print-level* 1]
         (pr-str value))))

(defmethod warning-message* ::misspelled-key [{:keys [val ::misspelled-key ::likely-misspelling-of] :as prob} value]
  (str "possible misspelled map key "
       (pr-str misspelled-key)
       " should probably be "
       (pr-str likely-misspelling-of)
       " in "
       (binding [*print-level* 1]
         (pr-str value))))

(defmethod warning-message* ::unknown-key [{:keys [val ::unknown-key] :as prob} value]
  (str "unknown map key "
       (pr-str unknown-key)
       " in "
       (binding [*print-level* 1]
         (pr-str value))))

(defn- handle-warnings [known-keys x problems]
  (#?@(:clj [binding [*out* *err*]]
       :cljs  [do])
    (doseq [prob (keep (partial enhance-spelling-problem known-keys) problems)]
      (*warning-handler*
       (assoc prob
              ::value x
              ::warning-message
              (warning-message* prob x))))))

(defn fuzzy-mapkeys-impl [known-keys keys-spec misspelled-keys]
  {:pre [(set? known-keys)]}
  (reify
    s/Specize
     (specize* [s] s)
     (specize* [s _] s)
    s/Spec
    (conform* [self x]
      (binding [*value* x]
        (let [result (s/conform* keys-spec x)]
          (if (and (not= ::s/invalid result)
                   (s/valid? misspelled-keys x))
            result
            (if (and *warn-only* (not (s/valid? misspelled-keys x)))
              (do
                (handle-warnings known-keys x (::s/problems (s/explain-data misspelled-keys x)))
                result)
              ::s/invalid)))))
    (unform* [_ x] (s/unform* keys-spec x))
    (explain* [_ path via in x]
      (binding [*value* x]
        (not-empty
         (vec
          (concat
           (s/explain* keys-spec path via in x)
           (if *warn-only*
             (handle-warnings known-keys x (s/explain* misspelled-keys path via in x))
             (keep
              (partial enhance-spelling-problem known-keys)
              (s/explain* misspelled-keys path via in x))))))))
    (gen* [_ a b c]
      (s/gen* keys-spec a b c))
    (with-gen* [_ gfn]
      (s/with-gen* keys-spec gfn))
    (describe* [_] (cons 'not-misspelled-keys (rest (s/describe* keys-spec))))))

(defn get-known-keys [{:keys [req opt req-un opt-un]}]
  (let [spec-specs  (into (set req) opt)
        un-specs    (into (set req-un) opt-un)]
    (into spec-specs
          (mapv #(-> % name keyword) un-specs))))

#?(:clj
   (defn in-cljs-compile? []
     (when-let [v (resolve 'cljs.env/*compiler*)]
       (thread-bound? v))))

#?(:clj
   (defn spec-ns-var [var-sym]
     (symbol
      (if (in-cljs-compile?)
       "cljs.spec.alpha"
       "clojure.spec.alpha")
      (name var-sym)))
   )

#?(:clj
   (defmacro keys
     "Use `spell-spec.alpha/keys` the same way that you would use
  `clojure.spec.alpha/keys` keeping in mind that the spec it creates
  will fail for keys that are misspelled.

  `spell-spec.alpha/keys` is a spec macro that has the same signature and
  behavior as clojure.spec.alpha/keys. In addition to performing the
  same checks that `clojure.spec.alpha/keys` does, it checks to see if
  there are unknown keys present which are also close misspellings of
  the specified keys.

  An important aspect of this behavior is that the map is left open to
  other keys that are not close misspellings of the specified
  keys. Keeping maps open is an important pattern in Clojure which
  allows one to simply add behavior to a program by adding extra data
  to maps that flow through functions. spell-spec.alpha/keys keeps
  this in mind and is fairly conservative in its spelling checks."
     [& args]
     ;; macroexpanding here to check args before using them later
     (let [form (macroexpand `(~(spec-ns-var 'keys) ~@args))
           known-keys (get-known-keys args)]
       `(spell-spec.alpha/fuzzy-mapkeys-impl
         ~known-keys
         ~form
         (~(spec-ns-var 'map-of) (spell-spec.alpha/not-misspelled ~known-keys) any?)))))

(defn warn-only-impl [spec]
  (reify
    s/Specize
     (specize* [s] (s/specize* spec))
     (specize* [s _] (s/specize* spec))
    s/Spec
    (conform* [_ x]
      (binding [*warn-only* true]
        (s/conform* spec x)))
    (unform* [_ x] (s/unform* spec x))
    (explain* [_ path via in x]
      (binding [*warn-only* true]
        (s/explain* spec path via in x)))
    (gen* [_ a b c] (s/gen* spec a b c))
    (with-gen* [_ gfn] (s/with-gen* spec gfn))
    (describe* [_] (s/describe* spec))))

#?(:clj
   (defmacro warn-keys
     "This macro is the same as `spell-spec.alpha/keys` macro except
  it will print warnings instead of failing when misspelled keys are discovered."
     [& args]
     `(spell-spec.alpha/warn-only-impl (spell-spec.alpha/keys ~@args))))

;; ----------------------------------------------------------------------
;; Strict keys
;; ----------------------------------------------------------------------
;; Strict keys fails on missing keys

#?(:clj
   (defmacro strict-keys
     "`strict-keys` is very similar to `spell-spec.alpha/keys` except
  that the map is closed to keys that are not specified.

  `strict-keys` will produce two types of validation problems: one for
  misspelled keys and one for unknown keys.

  This spec macro violates the Clojure idiom of keeping maps open. However,
  there are some situations where this behavior is warranted. I
  strongly advocate for the use of `spell-spec.alpha/keys` over
  `strict-keys`"
     [& args]
     ;; macroexpanding here to check args before using them later
     (let [form (macroexpand `(~(spec-ns-var 'keys) ~@args))
           known-keys (spell-spec.alpha/get-known-keys args)]
       `(spell-spec.alpha/fuzzy-mapkeys-impl
         ~known-keys
         ~form
         (~(spec-ns-var 'map-of)
          ~known-keys any?)))))

#?(:clj
   (defmacro warn-strict-keys
     "This macro is similar to `spell-spec.alpha/strict-keys` macro
  except that it will print warnings for unknown and misspelled keys
  instead of failing."
     [& args]
     `(spell-spec.alpha/warn-only-impl (spell-spec.alpha/strict-keys ~@args))))
