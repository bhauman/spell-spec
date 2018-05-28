(ns spell-spec.expound-test
  (:require [#?(:clj clojure.test :cljs cljs.test)
             :refer [deftest is testing]]
            [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha)
             :as s]
            [spell-spec.alpha :as spell :refer [warn-keys strict-keys warn-strict-keys]]
            [expound.alpha :as exp]
            [expound.ansi :as ansi]
            [spell-spec.expound]))

(deftest check-misspell-test
  (let [spec (spell/keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :barabara 1}
        result
        (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should probably be"))
    (is (.contains result " :hello\n"))))

(deftest check-misspell-with-namespace-test
  (let [spec (spell/keys :opt [::hello ::there])
        data {::there 1 ::helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should probably be"))
    (is (.contains result ":spell-spec.expound-test/hello\n"))))

(s/def ::hello integer?)
(s/def ::there integer?)

(deftest other-errors-test
  (let [spec (spell/keys :opt-un [::hello ::there])
        data {:there "1" :helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should probably be"))
    (is (.contains result " :hello\n"))

    (is (not (.contains result "Spec failed")))
    (is (not (.contains result "should satisfy")))
    (is (not (.contains result "integer?")))))

(deftest warning-is-valid-test
  (let [spec (warn-keys :opt-un [::hello ::there])
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
  (let [spec (warn-strict-keys :opt-un [::hello ::there])
        data {:there 1 :barabara 1}]
    (testing "expound prints warning to *err*"
      (binding [*err* (java.io.StringWriter.)]
        (exp/expound-str spec data)
        (is (= (str *err*)
               "SPEC WARNING: unknown map key :barabara in {:there 1, :barabara 1}\n"))))))

(deftest multiple-spelling-matches
  (let [spec (spell/keys :opt-un [::hello1 ::hello2 ::hello3 ::hello4 ::there])
        data {:there 1 :helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should probably be one of"))
    (doseq [k [:hello1 :hello2 :hello3 :hello4]]
      (is (.contains result (pr-str k)))))
  (let [spec (spell/keys :opt-un [::hello1 ::hello2 ::hello3 ::there])
        data {:there 1 :helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should probably be one of"))
    (is (not (.contains result (pr-str :hello4))))
    (doseq [k [:hello1 :hello2 :hello3]]
      (is (.contains result (pr-str k)))))
  (let [spec (spell/keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :barabara 1}
        result (exp/expound-str spec data)]
    (is (.contains result "Misspelled map key"))
    (is (.contains result "should probably be: :hello\n")))
  )

;; checking color
#_(ansi/with-color
    (exp/expound (s/map-of keyword? any?) {"hello" 1 :there 1}))

#_(ansi/with-color
    (exp/expound (check-misspelled-keys :opt-un [::hello ::there])
                 {:there 1 :helloo 1 :barabara 1}))

#_(ansi/with-color
    (exp/expound (strict-keys :opt-un [::hello ::there])
                 {:there 1 :barabara 1}))

