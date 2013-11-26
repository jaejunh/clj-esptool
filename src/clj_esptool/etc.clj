(ns clj-esptool.etc
  (:require [ clj-time.local :as loc ]
            [ clj-time.format :as tf ])
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 5. Statement 정의(Define Statement) 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sel-network-statement
  (str "SELECT * FROM LogDataEvent where level='info' and owner='eth1' "
       "output last every 1 seconds"))
;;      ))
(def engine-statement
  (str "select * from com.espertech.esper.client.metric.EngineMetric "))

(def a-statement
  (str "select ptime, server, program, level, count(*) as cnt, message "
        "from LogDataEvent.win:time(20 sec) "
        "where message like '%nm-dns-dnsmasq%' "
        "group by server, program, level "
        "having level='error' and count(*) > 1"))
;; count가 넘는 데이타를 모두 가져오기
(def b1-statement
  (str "select window(*), count(*) as b1cnt "
        "from LogDataEvent.win:time(0.1 sec) as b1 "
        "where b1.message like '%nm-dns-dnsmasq%' "
        "group by b1.level "
        "having b1.level='error' and count(*) > 1"))

(def b2-statement
  (str "select window(*) as win, count(*) as cnt "
        "from LogDataEvent.win:time(0.2 sec) as b2 "
        "where b2.owner='eth1' "
        "group by b2.level "
        "having b2.level='info' and count(*) > 3"))


