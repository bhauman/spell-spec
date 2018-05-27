(ns spell-spec.expound-test
  (:require [#?(:clj clojure.test :cljs cljs.test)
             :refer [deftest is testing]]
            [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha)
             :as s]
            [spell-spec.core :as sspc :refer [check-misspelled-keys warn-on-misspelled-keys strict-keys warn-on-unknown-keys]]
            [expound.alpha :as exp]
            [expound.ansi :as ansi]
            [spell-spec.expound]))

(deftest check-misspell-test
  (let [spec (check-misspelled-keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :barabara 1}
        result
        (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should be spelled"))
    (is (.contains result " :hello\n"))))

(deftest check-misspell-with-namespace-test
  (let [spec (check-misspelled-keys :opt [::hello ::there])
        data {::there 1 ::helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should be spelled"))
    (is (.contains result ":spell-spec.expound-test/hello\n"))))

(s/def ::hello integer?)
(s/def ::there integer?)

(deftest other-errors-test
  (let [spec (check-misspelled-keys :opt-un [::hello ::there])
        data {:there "1" :helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should be spelled"))
    (is (.contains result " :hello\n"))

    (is (.contains result "Spec failed"))
    (is (.contains result "should satisfy"))
    (is (.contains result "integer?"))))

(deftest warning-is-valid-test
  (let [spec (warn-on-misspelled-keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :barabara 1}]
    (testing "expound prints warning to *err*"
      (binding [*err* (java.io.StringWriter.)]
        (exp/expound-str spec data)
        (is (= (str *err*)
               "SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1, :barabara 1}\n"))))))

(deftest strict-keys-test
  (let [spec (strict-keys :opt-un [::hello ::there])
        data {:there 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Unknown map key"))
    (is (.contains result "should be one of"))
    (is (.contains result " :hello, :there\n"))))

(deftest  warn-on-unknown-keys-test
  (let [spec (warn-on-unknown-keys :opt-un [::hello ::there])
        data {:there 1 :barabara 1}]
    (testing "expound prints warning to *err*"
      (binding [*err* (java.io.StringWriter.)]
        (exp/expound-str spec data)
        (is (= (str *err*)
               "SPEC WARNING: unknown map key :barabara in {:there 1, :barabara 1}\n"))))))

;; checking color
#_(ansi/with-color
    (exp/expound (s/map-of keyword? any?) {"hello" 1 :there 1}))

#_(ansi/with-color
    (exp/expound (check-misspelled-keys :opt-un [::hello ::there])
                 {:there 1 :helloo 1 :barabara 1}))

#_(ansi/with-color
    (exp/expound (strict-keys :opt-un [::hello ::there])
                 {:there 1 :barabara 1}))

(s/explain (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
           {:there 1 :helloo 1})

(exp/expound (spell-spec.core/check-misspelled-keys :opt-un [::hello ::there]) 
             {:there 1 :helloo 1})

;; In: [:helloo 0] val: :helloo fails at: [0] predicate: (not-misspelled #{:hello :there})
;; 	 :expound.spec.problem/type  :spell-spec.core/misspelled-key
;; 	 :spell-spec.core/misspelled-key  :helloo
;; 	 :spell-spec.core/likely-misspelling-of  :hello
