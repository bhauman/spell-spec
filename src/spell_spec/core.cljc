(ns spell-spec.core
  (:require
   [#?(:clj clojure.spec.alpha
       :cljs cljs.spec.alpha)
    :as s])
  #?(:cljs (:require-macros [spell-spec.core :refer [check-misspelled-keys warn-on-misspelled-keys strict-keys warn-on-unknown-keys]])))

(def ^:dynamic *value* {})

(def ^:dynamic *warn-only* false)

(def ^:dynamic *length->threshold*)

(def default-warning-handler #(some->> % ::warning-message
                                      (str "SPEC WARNING: ")
                                      println))

(def ^:dynamic *warning-handler* default-warning-handler)

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

(defn levenshtein
  "Compute the levenshtein distance between two [sequences]."
  [sequence1 sequence2]
  (peek
    (reduce (fn [previous current] (next-row previous current sequence2))
            (map #(identity %2) (cons nil sequence2) (range))
            sequence1)))

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

(defn similar-key* [thresh ky ky2]
  (let [dist (levenshtein (str ky) (str ky2))]
    (when (<= dist thresh)
      dist)))

(defn similar-key [ky ky2]
  (let [min-len (apply min (map (comp count #(if (.startsWith % ":") (subs % 1) %) str) [ky ky2]))]
    (similar-key* (#?(:clj (if (bound? #'*length->threshold*)
                             *length->threshold*
                             length->threshold)
                      :cljs  length->threshold)
                   min-len) ky ky2)))

(defn likely-misspelled [known-keys]
  (fn [key]
    (and (not (known-keys key))
         (->> known-keys
              (filter #(similar-key % key))
              (remove (set (keys *value*)))
              not-empty))))

(defn not-misspelled [known-keys] (complement (likely-misspelled known-keys)))

(defn most-similar-to [key known-keys]
  (->> ((likely-misspelled known-keys) key)
       (map (juxt #(levenshtein (str %) (str key)) identity))
       (filter first)
       (sort-by first)
       first
       second))

(defn enhance-spelling-problem [known-keys {:keys [val] :as prob}]
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

(defn handle-warnings [known-keys x problems]
  (#?@(:clj [binding [*out* *err*]]
       :cljs  [do])
    (doseq [prob (keep (partial enhance-spelling-problem known-keys) problems)]
      (*warning-handler*
       (assoc prob
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

#_(symbol "clojure.spec.alpha"
          "keys")

#?(:clj
   (defmacro check-misspelled-keys
     "This is a spec that has the same signature as the clojure.spec.alpha/keys spec.
  The main difference is that it fails on keys that are likely misspelled.
  
  This spec will also provide an explanation for each misspelled key."
     [& args]
     ;; macroexpanding here to check args before using them later
     (let [form (macroexpand `(~(spec-ns-var 'keys) ~@args))
           known-keys (get-known-keys args)]
       `(spell-spec.core/fuzzy-mapkeys-impl
         ~known-keys
         ~form
         (~(spec-ns-var 'map-of) (spell-spec.core/not-misspelled ~known-keys) any?)))))

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
   (defmacro warn-on-misspelled-keys
     "This is a spec that has the same signature as the clojure.spec.alpha/keys spec.
  The main difference is that it WARNs on keys that are likely misspelled."
     [& args]
     `(spell-spec.core/warn-only-impl (spell-spec.core/check-misspelled-keys ~@args))))

;; ----------------------------------------------------------------------
;; Strict keys
;; ----------------------------------------------------------------------
;; Strict keys fails on missing keys

#?(:clj
   (defmacro strict-keys
     "This is a spec that has the same signature as the clojure.spec.alpha/keys spec.
  The main difference is that it fails on keys that are not present in
  the spec.
  
  This betrays a very important and helpful idiom in Clojure of
  allowing maps to be open you should only use this when you are
  absolutely certain that the set of possible keys is closed.

  I highly recommend that you use `check-misspelled-keys` instead of
  `strict-keys` as it still catches a majority of errors while
  allowing other keys to pass through.
  
  This spec will provide explanation data for each unknown key."
     [& args]
     ;; macroexpanding here to check args before using them later
     (let [form (macroexpand `(~(spec-ns-var 'keys) ~@args))
           known-keys (spell-spec.core/get-known-keys args)]
       `(spell-spec.core/fuzzy-mapkeys-impl
         ~known-keys
         ~form
         (~(spec-ns-var 'map-of)
          ~known-keys any?)))))

#?(:clj
   (defmacro warn-on-unknown-keys
     "This is a spec that has the same signature as the clojure.spec.alpha/keys spec.
  The main difference is that it WARNs on keys that are not present in
  the spec.
  
  This betrays a very important and helpful idiom in Clojure of
  allowing maps to be open you should only use this when you are
  absolutely certain that the set of possible keys is closed.

  I highly recommend that you use `warn-on-misspelled-keys` instead of
  `warn-on-unknown-keys` as it still catches a majority of errors while
  allowing other keys to pass through."
     [& args]
     `(spell-spec.core/warn-only-impl (spell-spec.core/strict-keys ~@args))))
