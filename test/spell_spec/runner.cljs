(ns ^:figwheel-hooks spell-spec.runner
  (:require
   [spell-spec.alpha-test]
   [spell-spec.expound-test]
   [cljs-test-display.core]
   [cljs.test]))

(defn ^:after-load test-all []
  (cljs.test/run-tests
   (cljs-test-display.core/init!)
   'spell-spec.alpha-test
   'spell-spec.expound-test))

(defn -main [& args] (test-all))
