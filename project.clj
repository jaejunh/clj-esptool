(defproject clj-esptool "0.1.0-SNAPSHOT"
  :description "Esper Command Line Tool with Clojure 
		(Thanks to ESP with Esper Tutorial and ZeroMQ Tutorial 
		on http://www.patternhatch.com/)"
  :url "http://blog.embian.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
		 [org.clojure/data.json "0.2.3"]
		 [org.clojure/tools.namespace "0.2.4"]
		 [clj-time "0.6.0"] ;; clj-time
                 [com.rmoquin.bundle/jeromq "0.2.0"] ;;jeromq
                 [cheshire "5.2.0"]                  ;;jeromq
                 [com.espertech/esper "4.10.0" :exclusions [log4j]]]
  :main clj-esptool.core)
