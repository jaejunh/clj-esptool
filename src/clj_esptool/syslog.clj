(ns clj-esptool.syslog
  (:require [ clj-time.local :as loc ]
            [ clj-time.format :as tf ])
)

(defn parse-owner
  [ s ]
  (let [ owner-clause (first (re-seq #"^\((\S+)\)(.+)" s)) ]
    (if (empty? owner-clause)
      [ "" s ]
      (rest owner-clause))))

(defn parse-strs
  [ s ]
  (let [ level-clause (first (re-seq #"^<(\S+)> (.+)" s))]
        (if (empty? level-clause)
            (cons "" (parse-owner s))
            (cons (second level-clause) (parse-owner (nth level-clause 2))))))

(defn parse-line
  [ line ]
  (let [year "2013"
        [mon dd hh-mm-ss server procs s] (clojure.string/split line #" " 6)
        ptime (clojure.string/join " " [year mon dd hh-mm-ss])
        [program & pids] (clojure.string/split procs #"[\:\[\]]" 3)
        [level owner message ] (parse-strs s)]

       {"ptime" ptime
        "server" server
        "program" program
        "pid" (first pids)
        "level" level
        "owner" owner
        "message" message }))

(defn print-lines
  [ filename ]
  (let [ [& lines ] (clojure.string/split (slurp filename) #"\n")]
    (doseq [line lines]
        (println (take 7 (parse-line line))))))



(defn log-init-from-file
  [ filename ]
        (map parse-line (clojure.string/split (slurp filename) #"\n")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn locale-date
  [ year mon dd hh-mm-ss ]
  (let [ us (java.util.Locale/US)
         from (java.text.SimpleDateFormat. "yyyy MMM dd HH:mm:ss" us)
         to (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss" us) ]
        (.format to (.parse from (clojure.string/join " " [ year mon dd hh-mm-ss ] )))))



