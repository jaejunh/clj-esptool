
(ns clj-esptool.esper
  (:require [ clj-time.local :as loc ]
	    [ clj-time.format :as tf ])
  (:import [com.espertech.esper.client EPServiceProvider
				       Configuration
				       StatementAwareUpdateListener
                                       UpdateListener
                                       EPServiceProviderManager]
	   [com.espertech.esper.core.service EPServiceProviderImpl]
))
 
(def attr-types
 {:int Integer
  :long Long
  :double Double
  :string String})
 
(defn new-event
  [event-name attrs]
  (let [event {:name event-name :attrs {}}]
    (reduce (fn [m [k v]] (assoc-in m [:attrs (name k)] (attr-types v))) 
	event attrs)))
 
(defn configuration
  [& events]
  (let [config (Configuration.)
;	reporting (.getMetricsReporting (.getEngineDefaults config))
	]
    ;; general configuration parameter
;<engine-settings>
;  <defaults>
;    <metrics-reporting enabled="true" engine-interval="1000" statement-interval="1000" 
;        threading="true"/>
;  </defaults>
;</engine-settings>
;    (.setEnableMetricsReporting reporting true)
 ;   (.setEngineInterval reporting 1000)
;    (.setStatementInterval reporting 1000)
;    (.setThreading reporting true)
    (doseq [{:keys [name attrs]} events]
      (.addEventType config name attrs))
    config))


;; (do-statement service #(.stop %1) k) 
;; (do-statement service #(.start %1) k) 
;; (do-statement service #(.destroy %1) k) 
;; (do-statement service #(.getName %1) k) 
;; (do-statement service #(.getState %1) k) 
;; (do-statement service #(.getText %1) k) 
;; (do-statement service #(.getAnnotations %1) k) 
;; (do-statement service get-statement-member k)


(defn get-statement-member
  [statement]
  (if-not (nil? statement)
      (if-not (nil? statement)
        (hash-map :name (.getName statement) 
		    :epl (.getText statement) 
		    :created (.getTimeLastStateChange statement)
		    :status (.toString (.getState statement))))))

(defn do-statement
  [service func k]

  (doall (map #(let [ ;;_ (println %1) 
		statement (.getStatement (.getEPAdministrator service) %1) ]
	   (func statement))
        (filter #(or (= % k) (= "all" k))  
	   (.getStatementNames (.getEPAdministrator service)))))) 
	   
(defn get-event-type
  [service event-name]
    (if (nil? event-name) 
      event-name
      (let [ config (.getConfiguration (.getEPAdministrator service)) 
    	   event-type (.getEventType config event-name)]
      (if-not (nil? event-type)
	(map #(hash-map :name %1 :type 
                  (last (clojure.string/split 
		          (.toString (.getPropertyType event-type %1)) #"\."))) 
		(.getPropertyNames event-type))))))
   
(defn list-event-types
  [service & arg]
    (let [ config (.getConfiguration (.getEPAdministrator service)) 
    	   event-types (.getEventTypes config)]
      (if-not (nil? event-types)
	(map #(hash-map :name (.getName %1) 
			:id (.getEventTypeId %1)
			:used-by (.toString (.getEventTypeNameUsedBy config (.getName %1)))) 
	     event-types))))
	

(defn add-event-type
  [service & events]
    (let [ config (.getConfiguration (.getEPAdministrator service))]
    	(doseq [{:keys [name attrs]} events]
      		(.addEventType config name attrs))
	(get events :name)))

(defn remove-event-type
  [service event-name]
    (let [ config (.getConfiguration (.getEPAdministrator service))]
	  (.removeEventType config event-name false)
	  event-name))


;; use-service?
(defn create-service
  [service-name config]
  (EPServiceProviderManager/getProvider service-name config))


(defn list-service
  [ service-name ]
  (map #(hash-map :name %1) 
	(filter #(or (= service-name %1) (= "all" service-name))
			 (EPServiceProviderManager/getProviderURIs)))) 

(defn get-service-instance
  [ service-name ]
  (EPServiceProviderManager/getProvider service-name))
  

(defn remove-service
  [ service-name ]
  (let [service (get-service-instance service-name)] 
     (if-not (nil? service) (.destroy service))))
	

(defn destroy-service
  [service]
  (.destroy service))
 
(defn new-statement
  ([service statement] (new-statement service statement (str "t" (rand-int 9999)))) 
  ([service statement k]
    (let [admin (.getEPAdministrator service)]
      (.createEPL admin statement ^:string k))))


(defn send-event
  [service event event-type]
  (.sendEvent (.getEPRuntime service) event event-type))

(defn render-event
  [service event]
  (let [ jsonRenderer (.getJSONRenderer (.getEventRenderer (.getEPRuntime service))
                                        (.getEventType event)) ]
    (.render jsonRenderer "EventResult" event)))


(defn statement-handler-json
  [new-events old-events statement service]
        (println "==>Triggered(" (.getName statement) ":" 
        	(.toString (loc/format-local-time (loc/local-now) :t-time)) ")" )
  	(println   
		(clojure.string/replace (.getText statement) #"(?i)(select|from|where|group by|having)" "\n\t\t$1"))
        (doseq [e new-events]
                (if-not (nil? e)
                        (do
			  (println "[New-Event]\n"
				 (render-event service e)))))
        (doseq [e old-events]
                (if-not (nil? e)
                        (do
			  (println "\t\t\t\t|[Old-Event]\n\t\t\t\t|" 
				(clojure.string/replace (render-event service e) #"[\n]" "\n\t\t\t\t|"))))))

(defn create-listener
  [listener]
  (proxy [UpdateListener] []
    (update [newEvents oldEvents]
      (apply listener newEvents oldEvents))))

(defn create-listener2
  [listener]
  (proxy [StatementAwareUpdateListener] []
    (update [newEvents oldEvents statement service]
	(listener newEvents oldEvents statement service)))) 

(defn add-listener
  [statement listener]
  (.addListener statement listener))
 
(defn remove-listener
  [statement listener]
  (.removeListener statement listener))

