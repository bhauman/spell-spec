(defproject com.bhauman/spell-spec "0.1.2-SNAPSHOT"
  :description "Clojure Spec macros which verify that unspecified map keys are not misspellings of specified map keys."
  :url "https://github.com/bhauman/spell-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm { :name "git"
         :url "https://github.com/bhauman/spell-spec"}

  :dependencies [[org.clojure/clojure "1.9.0"]]

  :clean-targets ^{:protect false} ["target"]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.238"]
                                  [org.clojure/test.check "0.9.0"]
                                  [com.bhauman/figwheel-main "0.1.2"]
                                  [com.bhauman/cljs-test-display "0.1.0"]
                                  [expound "0.7.0"]]
                   :source-paths ["src" "example"]
                   :resource-paths ["target"]}}

  :aliases {"test-cljs" ["do" "clean" ["run" "-m" "figwheel.main" "-m" "spell-spec.runner"]]
            "auto-test-cljs" ["do" "clean"
                              ["run"
                               "-m"
                               "figwheel.main"
                               "-w" "src"
                               "-w" "test"
                               "-e" "(require 'spell-spec.runner)(spell-spec.runner/test-all)"
                               "-c"
                               "spell-spec.runner"
                               "-r"]]}
  )
