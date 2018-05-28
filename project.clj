(defproject com.bhauman/spell-spec "0.1.0-SNAPSHOT"
  :description "Clojure Spec macros which verify that unspecified map keys are not misspellings of specified map keys."
  :url "https://github.com/bhauman/spell-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm { :name "git"
         :url "https://github.com/bhauman/spell-spec"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[expound "0.6.1-SNAPSHOT"]]}})
