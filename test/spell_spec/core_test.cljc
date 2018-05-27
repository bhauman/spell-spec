(ns spell-spec.core-test
  (:require [#?(:clj clojure.test :cljs cljs.test)
             :refer [deftest is testing]]
            [#?(:clj  clojure.spec.alpha
                :cljs cljs.spec.alpha)
             :as s]
            [spell-spec.core :as sspc :refer [check-misspelled-keys warn-on-misspelled-keys strict-keys warn-on-unknown-keys]]))

(deftest check-misspell-test
  (let [spec (check-misspelled-keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :barabara 1}
        {:keys [::s/problems]}
        (s/explain-data spec data)]
    (is (not (s/valid? spec data)))
    (is (= 1 (count problems)))
    (when-let [{:keys [:expound.spec.problem/type ::sspc/misspelled-key ::sspc/likely-misspelling-of]} (first problems)]
      (is (= ::sspc/misspelled-key type))
      (is (= misspelled-key :helloo))
      (is (= likely-misspelling-of :hello)))))

(deftest check-misspell-with-namespace-test
  (let [spec (check-misspelled-keys :opt [::hello ::there])
        data {::there 1 ::helloo 1 :barabara 1}
        {:keys [::s/problems]}
        (s/explain-data spec data)]
    (is (not (s/valid? spec data)))
    (is (= 1 (count problems)))
    (when-let [{:keys [:expound.spec.problem/type ::sspc/misspelled-key ::sspc/likely-misspelling-of]} (first problems)]
      (is (= ::sspc/misspelled-key type))
      (is (= misspelled-key ::helloo))
      (is (= likely-misspelling-of ::hello)))))

(s/def ::hello integer?)
(s/def ::there integer?)

(deftest other-errors-come-first
  (let [spec (check-misspelled-keys :opt-un [::hello ::there])
        data {:there "1" :helloo 1 :barabara 1}
        {:keys [::s/problems]} (s/explain-data spec data)]
    (is (not (s/valid? spec data)))
    (is (= 2 (count problems)))
    (when (= 2 (count problems))
      (is (= (-> problems first :val) "1"))
      (is (= (-> problems second ::sspc/misspelled-key) :helloo)))))

(deftest warning-is-valid-test
  (let [spec (warn-on-misspelled-keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :barabara 1}]
    (is (s/valid? spec data))
    (is (nil? (s/explain-data spec data)))

    (testing "valid prints to *err*"
      (binding [*err* (java.io.StringWriter.)]
        (s/valid? spec data)
        (is (= (str *err*) "SPEC WARNING: possible misspelled map key :helloo should probably be :hello in {:there 1, :helloo 1, :barabara 1}\n"))))

    (testing "other errors still show up"
      (is (not (s/valid? spec {:there "1" :helloo 1})))
      (let [{:keys [::s/problems]} (s/explain-data spec {:there "1" :helloo 1})]
        (is (= 1 (count problems)))
        (is (= "1" (-> problems first :val)))))))

(deftest dont-recommend-keys-if-key-already-present-in-value
  (let [spec (check-misspelled-keys :opt-un [::hello ::there])
        data {:there 1 :helloo 1 :hello 1 :barabara 1}]
    (is (s/valid? spec data))
    (is (nil? (s/explain-data spec data)))
    (testing "should not warn if key present in value already"
      (binding [*err* (java.io.StringWriter.)]
        (s/valid? spec data)
        (is (= (str *err*) ""))))))

(deftest strict-keys-test
  (let [spec (strict-keys :opt-un [::hello ::there])
        data {:there 1 :barabara 1}
        {:keys [::s/problems]}
        (s/explain-data spec data)]
    (is (not (s/valid? spec data)))
    (is (= 1 (count problems)))
    (when-let [{:keys [:expound.spec.problem/type ::sspc/unknown-key]} (first problems)]
      (is (= ::sspc/unknown-key type))
      (is (= unknown-key :barabara)))))

(deftest strict-keys-misspelled-test
  (testing "should also include misspelled errors"
    (let [spec (strict-keys :opt-un [::hello ::there])
          data {:there 1 :helloo 1 :barabara 1}
          {:keys [::s/problems]}
          (s/explain-data spec data)]
      (is (not (s/valid? spec data)))
      (is (= 2 (count problems)))
      (when-let [{:keys [:expound.spec.problem/type ::sspc/misspelled-key ::sspc/likely-misspelling-of]} (first problems)]
        (is (= ::sspc/misspelled-key type))
        (is (= misspelled-key :helloo))
        (is (= likely-misspelling-of :hello)))
      (when-let [{:keys [:expound.spec.problem/type ::sspc/unknown-key]} (second problems)]
        (is (= ::sspc/unknown-key type))
        (is (= unknown-key :barabara))))))

(deftest warn-on-unknown-keys-test
  (let [spec (warn-on-unknown-keys :opt-un [::hello ::there])
        data {:there 1 :barabara 1}
        {:keys [::s/problems]}
        (s/explain-data spec data)]
    (is (s/valid? spec data))
    (is (nil? (s/explain-data spec data)))
    (testing "should warn if unknown-key"
      (binding [*err* (java.io.StringWriter.)]
        (s/valid? spec data)
        (is (= (str *err*) "SPEC WARNING: unknown map key :barabara in {:there 1, :barabara 1}\n"))))))
