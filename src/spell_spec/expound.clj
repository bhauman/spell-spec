(ns spell-spec.expound
  (:require
   [clojure.string :as string]
   [expound.alpha :as exp]
   [expound.ansi :as ansi]
   [expound.printer :as printer]
   [expound.problems :as problems]))

(defn exp-formated [header _type spec-name val path problems opts]
  (printer/format
   "%s\n\n%s\n\n%s"
   (#'exp/header-label header)
   (printer/indent (#'exp/*value-str-fn* spec-name val path (problems/value-in val path)))
   (exp/expected-str _type spec-name val path problems opts)))

(defmethod exp/problem-group-str :spell-spec.alpha/misspelled-key [_type spec-name val path problems opts]
  (exp-formated "Misspelled map key"  _type spec-name val path problems opts))

(defmethod exp/expected-str :spell-spec.alpha/misspelled-key [_type spec-name val path problems opts]
  (let [{:keys [:spell-spec.alpha/misspelled-key :spell-spec.alpha/likely-misspelling-of]} (first problems)]
    (str "should be spelled\n\n"
         (printer/indent
          (ansi/color (pr-str likely-misspelling-of)
                      :good)))))

(defmethod exp/problem-group-str :spell-spec.alpha/unknown-key [_type spec-name val path problems opts]
  (exp-formated "Unknown map key"  _type spec-name val path problems opts))

(defmethod exp/expected-str :spell-spec.alpha/unknown-key [_type spec-name val path problems opts]
  (let [{:keys [:spell-spec.alpha/unknown-key pred]} (first problems)]
    (str "should be" (when (> (count pred) 1) " one of")  "\n\n"
         (printer/indent (string/join ", " (map #(ansi/color (pr-str %) :good) pred) )))))



