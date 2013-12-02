(use 'clj-esptool.core)
(use 'clj-esptool.esper)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A.2. Output for Un-aggregated and Un-grouped Queries
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;;

;; assumption
;; your program is using "log-service" (global)

;; change:  2013-10-17 event definition can be handle by remote server ^^


;; event defition 
(def k [ "symbol", "volume", "price", "omit" ])
(def market-data-property {"symbol" :string, 
				"volume" :int,
				"price" :double,
				"omit" :string })

;; register event "locally" for shoot!
;; omit:  (def market-data (new-event "MarketData" market-data-property))

;; send register event "remotely" for server!

(esp :help)

;; as-of 2013.11.01
(esp :use-service)  ; current service =>"default"
(esp :add-service "test1")
(esp :list-service) 
(esp :use-service "test1")  ; current service => "test"

(esp :list-type)
(esp :add-type "MarketData" market-data-property)  
(esp :list-type)
(esp :display-type "MarketData")

;; register epl 

(def a-2-4 "select irstream symbol, volume, price from MarketData.win:time(5.50 sec) Output first every 1 seconds")


(esp :list)
(esp :add a-2-4)
(esp :list)
(esp :list-type)

;; to test whether stop/start works
(esp :stop :all) (esp :list) (esp :start :all) (esp :list)

;; now, send data
(defn mytest 
  "test a-2-4"
  []
(esp :shoot-init)
(esp :shoot 0.2 "MarketData" k [ "IBM", 100, 25.0, "[IBM, 100, 25.0]" ]) 
(esp :shoot 0.6 "MarketData" k [ "MSFT",5000, 9.0, "[MSFT, 5000, 9.0]"]) 
(esp :shoot 0.7 "MarketData" k [ "IBM", 150, 24.0, "[IBM, 150, 24.0]" ]) 
(esp :shoot 0.0 "MarketData" k [ "YAH", 10000, 1.0, "[YAH, 10000, 1.0]" ]) 
(esp :shoot 0.6 "MarketData" k [ "IBM", 155, 26.0, "[IBM, 155, 26.0]" ]) 
(esp :shoot 1.4 "MarketData" k [ "YAH", 11000, 2.0, "[YAH,11000,2.0]" ]) 
(esp :shoot 0.8 "MarketData" k [ "IBM", 150, 22.0, "[IBM, 150, 22.0]" ]) 
(esp :shoot 0.6 "MarketData" k [ "YAH", 11500, 3.0, "[YAH, 11500, 3.0]" ]) 
(esp :shoot 1.0 "MarketData" k [ "YAH", 10500, 1.0, "[YAH, 10500, 1.0]" ]) 
(println "Sleeping 5.5 second for windows")
(Thread/sleep 5500)


;; clean-up

(esp :list-type)
(esp :remove :all) 
(esp :list)

(esp :remove-type "MarketData") 
(esp :list-type)

(esp :remove-service "test1") 
(esp :use-service "default")
;(destroy-service log-service)
;(future-cancel esp-future)
;(System/exit 0)
)

;; help
;; TODO: need more clean up on symbols: market-data-property, k, a-2-4
(println " ")
(println " ")
(println ".......  Please Type (mytest) to run this test " )
(println ".......  epl: " a-2-4)
(println " ")

