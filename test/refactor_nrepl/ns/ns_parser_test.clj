(ns refactor-nrepl.ns.ns-parser-test
  (:require [refactor-nrepl.ns.ns-parser :refer :all]
            [clojure.test :refer :all]))

(deftest parses-libspecs-with-prefix-vectors-containing-specs
  (is (= (list {:ns 'compojure.core :refer '[defroutes] :reload-all true}
               {:ns 'compojure.route :as 'route :reload true}
               {:ns 'website.github-manager :as 'gman :verbose true})
         (get-libspecs '(ns refactor-nrepl.test
                          (:require [compojure
                                     [core :refer [defroutes] :reload-all true]
                                     [route :as route :reload true]]
                                    [website.github-manager :as gman
                                     :verbose true])
                          (:gen-class))))))

(deftest parses-libspecs-with-prefix-vectors-without-specs
  (is (= (list {:ns 'compojure.core}
               {:ns 'compojure.route})
         (get-libspecs '(ns refactor-nrepl.test
                          (:require [compojure core route]))))))

(deftest parse-imports-without-prefix-list
  (is (= (list 'java.util.Date 'java.util.Calendar)
         (get-imports '(ns refactor-nrepl.ns.ns-parser-test
                         (:require [refactor-nrepl.ns.ns-parser :refer :all]
                                   [clojure.test :refer :all])
                         (:import java.util.Date java.util.Calendar))))))

(deftest parses-imports-with-prefix-list
  (is (= (list 'java.util.Date 'java.util.Calendar)
         (get-imports '(ns refactor-nrepl.ns.ns-parser-test
                         (:require [refactor-nrepl.ns.ns-parser :refer :all]
                                   [clojure.test :refer :all])
                         (:import [java.util Date Calendar]))))))

(deftest parses-require-macros
  (is (= '({:ns cljs.test :refer-macros [deftest is]}
           {:ns cljs.test :require-macros [testing run-tests]})
         (get-libspecs '(ns test
                          (:require [cljs.test :refer-macros [deftest is]])
                          (:require-macros [cljs.test :refer [testing run-tests]]))))))
