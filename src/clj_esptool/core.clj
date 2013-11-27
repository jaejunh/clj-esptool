(ns clj-esptool.core
  (:use [ clj-esptool.esper ] 
	[ clj-esptool.syslog ]
	[ clj-esptool.etc ]
	[ clojure.pprint ]  
	[ clojure.tools.namespace.repl :only (refresh)] )
  (:import [org.jeromq ZMQ])      ;;jeromq
  (:require  (cheshire [core :as c])
	     [ clj-time.local :as loc ]
             [ clj-time.format :as tf ])
) 

	
 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 1. Define Data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def log-data-event
        (new-event "LogDataEvent"
                { "ptime" :string   
                  "server" :string  
                  "program" :string 
                  "pid" :int         
                 "level" :string   
                  "owner" :string    
                  "message" :string }))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 2. Define Enumerators 
;; 3. Define Service 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ctx (ZMQ/context 1))
(def shoot-now (atom (System/currentTimeMillis)))
(def sid (atom 0))

(def default-service (atom nil))
 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 4. Define MAIN/Demo 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; The Publish-Subscribe Pattern
;
; Server
(defn log-data-publisher
  []
  (let [filename "doc/syslog.2"
        s (.socket ctx ZMQ/PUB)
        sample-log-data-event (log-init-from-file filename) ]
    (.bind s "tcp://127.0.0.1:6666")
    (println "entering loop....")
    (while :true
       (doseq [e sample-log-data-event]
        (.send s (c/generate-string e))
	(Thread/sleep 10)))))


  
(defn print-all
  []
  (let [sample-log-data-event (log-init-from-file) ]
        (doseq [e sample-log-data-event]
                (println e))))

(defn make-hash
 [a b r]
 (if (nil? (first a))
    r
    (make-hash (rest a) (rest b) (assoc r (first a) (first b))))) 

(defn shoot-init [] 
	(reset! shoot-now (System/currentTimeMillis)))

(defn  shoot
 [ service defer e f data ] 
  { :pre (number? defer) }
  (let [elapsed (format "%.1f" (/ (- (System/currentTimeMillis) @shoot-now) 1000.0))
	int-defer (int (* defer 100))]
    (if (> int-defer 0) 
	(do  
		(println "At D+" elapsed  ":  [noop].....................................")
		(Thread/sleep 100)
		(shoot service (- defer 0.1) e f data))
  	(let [ ev (make-hash f data {}) ]
  	  (println "At D+" elapsed ": [Send Data] " 
		(dissoc ev "omit") " ===========================>")
  	  (send-event service ev e))))) 

(defn elapsed-event-handler-json
  [new-events old-events statement service]
  ;;(let [elapsed (/ (- (System/currentTimeMillis) @shoot-now) 100.0) ]
  (let [elapsed (format "%.2f" (/ (- (System/currentTimeMillis) @shoot-now) 1000.0)) ]
        (println "==>At D+" elapsed ", Triggered(" (.getName statement) ":"
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
                                (clojure.string/replace (render-event service e) #"[\n]" "\n\t\t\t\t|")))))))
 
(defn try-shoot 
  [ service defer arg ]
  (let [ { e "aux1" f "aux2"  data "aux3" }  arg
   	 r (try
		(shoot service defer e f data)
		(catch Exception e
		   (println "^^^^shoot exception:" 
			(first (clojure.string/split (.getMessage e) #"\n")))))]
     { :r r } ))


(defn try-statement 
  [service f arg]
  (let [ 
	r (try
             (do-statement service f arg)
             (catch Exception e
                  (println "^^^^statement Exception:"
                      (first (clojure.string/split (.getMessage e) #"\n")))))]
	{ :r r }))


(defn try-new-statement
  [service f arg]
  (let [ k (str "s" (swap! sid inc))
         _ (try 
		(cond (nil? (re-seq #"^(?i)\s*create\s+" arg)) 
			(add-listener (new-statement service arg k) (create-listener2 f))	
		      :else
			(new-statement service arg k))
		(catch Exception e 
		   (println "^^^^statement exception:" 
			(first (clojure.string/split (.getMessage e) #"\n")))))]
	{ :r k }))

   
(defn try-type
  [service f arg]
  (let [  r (try 
		(f service arg)
		(catch Exception e
		   (println "^^^^type exception:" 
			(first (clojure.string/split (.getMessage e) #"\n")))))]
	{ :r r }))
	


(defn use-service
  [arg]
  (let [service (get-service-instance arg)]
    (if-not (nil? service) (reset! default-service service))))

(defn try-service
  [f arg]
  (let [ r (try
		(f arg)
		(catch Exception e
		  (println "^^^^service exception:"
			(first (clojure.string/split (.getMessage e) #"\n")))))]
	{ :r r }))




(defn esp-handler
 []
  (let [ s (.socket ctx ZMQ/REP) 
	 b (try 
	      (.bind s "tcp://127.0.0.1:5555")
	      (catch Exception e
		(println "^^^^jeromq bind exception:"
			(first (clojure.string/split (.getMessage e) #"\n")))))]
	
   (cond (< b 0) 
		(println "\n\n\n^^^^Error Connection tcp://127.0.0.1:5555. Exiting..\n\n"
		  "\t ==> I can only run CLIENT!!!\n\n")
    :else (do
    (reset! default-service (create-service "default" 
			(configuration log-data-event)))
    (println "\n\n\n!!! esp-handler " (.getURI @default-service) 
			" at tcp://127.0.0.1:5555.. READY OK!!.n\n")
    
    (loop [msg (.recv s)]
      (let [ {cmd "cmd" arg "arg" aux "aux"} (c/parse-string (String. msg)) ]
	(println "============================================================================")
	(println "<= Server Received:\t" cmd  " (" arg ","  aux ")"  )
	(.send s (c/generate-string  
	  (case cmd
	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  ;;  Part 1:  Event Type Commands
	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  "add-type"
	    (let [ 
		   keyed-aux (reduce #(assoc %1 (first %2) (keyword (second %2))) {}  aux) 
		   event-type (new-event arg keyed-aux) ]
			(try-type @default-service #(add-event-type %1 %2) event-type)
	  		(try-type @default-service #(list-event-types %1 %2) (event-type :name)))

	  "remove-type"	(do (try-type @default-service #(remove-event-type %1 %2) arg)
	  		    (try-type @default-service #(list-event-types %1 %2) arg))
				
		
	  "display-type" (try-type @default-service #(get-event-type %1 %2) arg)
	    
	  "list-type"	(try-type @default-service #(list-event-types %1 %2) nil)

	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  ;;  Part 2:  Statement Commands
	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

	  ;"add"		(let [ r (try-new-statement @default-service statement-handler-json arg) ]
	  "add"		(let [ r (try-new-statement @default-service elapsed-event-handler-json arg) ]
			(try-statement @default-service #(get-statement-member %) (get r :r)))

	  "remove"	(do (try-statement @default-service #(.destroy %) arg)
	  		    (try-statement @default-service #(get-statement-member %) arg))

	  "list"	(try-statement @default-service #(get-statement-member %) "all")

	  "display"	(try-statement @default-service #(get-statement-member %) arg)

	  "stop" 	(do (try-statement @default-service #(.stop %) arg)
	  		    (try-statement @default-service #(get-statement-member %) arg))

	  "start" 	(do (try-statement @default-service #(.start %) arg)
	  		    (try-statement @default-service #(get-statement-member %) arg))

	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  ;;  Part 3:  Send Data Commands
	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  "shoot-init" 	{ :r (shoot-init) }

	  "shoot"	(try-shoot @default-service arg aux)

	
	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  ;;  Part 4:  Service Commands 
	  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
	  "add-service" (do (try-service #(create-service %1 (configuration)) arg)
			    (try-service list-service arg))
	  "remove-service" (do (try-service remove-service arg) 
			       (try-service use-service "default")
			       { :r nil }) 
	  "list-service" (try-service list-service "all") 
	  "use-service" (if-not (nil? arg) 
			    (do (try-service use-service arg)  
			        (try-service list-service arg))
			    (try-service list-service (.getURI @default-service)))

	  "default")))
	(println "..."))  ;; end of let

      (recur (.recv s)))
      (println "Terminating esp-handler!")))))



; Client

(defn demo 
  ( [ num-events ] (demo num-events @default-service))
  ( [ num-events service ] 
    (let [s (.socket ctx ZMQ/SUB)
                i (atom 0) ]
      (.subscribe s "")
      (.connect s "tcp://127.0.0.1:6666")

      (dotimes [_ num-events]
        (if (= (mod @i 10) 0)
          (println "===> Sending Event " @i "...")) 
        (send-event service 
          (->> (.recv s) (String. ) (c/parse-string)) 
	  "LogDataEvent")
        (swap! i inc))

      (.close s))))

(defn esp-help
  []
  (println "
	===============================================================================
	You can use following (esp :command-xxx ....)
	===============================================================================
	
	1. Event-Type Definition Related Commands:
		:list-type, :add-type, :remove-type, :display-type, 

	2. Statement Related Commands:
		:list,  :add, :remove, :display, :stop, :start

	3. Sending Event Commands:
		:shoot-init, :shoot


 (*new)	4. Sevice Commands:
		:list-service, :add-service, :remove-service, :use-service
	===============================================================================
	Examples)
	===============================================================================

	(esp)     ;; => show help!  (esp:help)
	(esp :help)
	
	(esp :list-type)
	(esp :add-type \"MarketData\" {\"symbol\" :string, \"price\": double, \"volume\": int, \"omit\" :string})
	(esp :remove-type \"MarketData\") 
	(esp :remove-type :all) (esp :remove-type \"all\")
	(esp :display-type \"MarketData\")
	
	(esp :list)
	(esp :add \"select * from MarketData\")
	(esp :remove \"s01\")
	(esp :remove :all) (esp :remove \"all\")
	(esp :display \"s01\")
	(esp :stop  :all) (esp :stop \"all\")
	(esp :start :all) (esp :start \"all\")


	(esp :shoot-init)
	(esp :shoot 
		0.2   				;; shoot delay second
	    	\"Market-data\" 		;; event-type
		[\"symbol\", \"volume\", \"price\", \"omit\" ]	;; event properties
		[\"IBMi\", 100, 25.0, \"some-string\"])  ;; event data 

	(esp :list-service)
	(esp :add-service \"test\")
	(esp :use-service)
	(esp :use-service \"test\")
	(esp :use-service)
	(esp :remove-service \"test\")
  "))



	
	
(defn esp 
  ( [] (esp-help))
  ( [ cmd ] (esp cmd nil nil)) 
  ( [ cmd arg ] (esp cmd arg nil))
  ( [ cmd arg aux1 aux2 aux3 ]
	(esp cmd arg { :aux1 aux1 :aux2 aux2 :aux3 aux3 })) 
  ( [ cmd arg aux]  
    {:pre [ (some #{cmd} [:add :remove :list :display 
			  :stop :start 
			  :add-type :remove-type :list-type :display-type 
			  :shoot-init :shoot 
			  :add-service :remove-service :list-service :use-service
			  :help ])]}
     (cond 
	;; put local or test function here
	(= cmd :help) (esp-help)
	(= cmd :new-command) (println cmd " not implemented!!!!!!!!!!")
	:else
	;; handling remote function
     	(let [s (.socket ctx ZMQ/REQ)]
	  (.connect s "tcp://127.0.0.1:5555")
	  ; (println (c/generate-string { :cmd cmd :arg arg :aux aux }))
	  (.send s (c/generate-string { :cmd cmd :arg arg :aux aux }))
	  (let [r (c/parse-string (String. (.recv s)))]
	    (.close s)
	    (println "=> Server Replied:\t") (println (get r "r"))
	    (println "============================================================================")
;	    (if (or (= cmd :list) (= cmd :list-type) (= cmd :display-type) (= cmd :display) (= cmd :use-service) (= cmd :list-service)) 
	    (if-not (or (= cmd :shoot) (= cmd :shoot-init))
		(do (println "Esper Has Following " cmd) 
		    (clojure.pprint/print-table   (get r "r"))))
	  (println ""))))))


(def esp-future (future (esp-handler)))


;; unblock followings if you need to modify thrower
;;(future-call log-data-publisher)
