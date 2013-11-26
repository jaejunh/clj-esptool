# Esper Command Tool and Example 

Learn to use Esper!

---

Table of Contents

* Getting Started  
* Using clj-esptool with Leiningen 
* First Demo: "select * from LogDataEvent" 
* Commands Examples 

---

## Getting Started

### Prerequisites

First, you need `java` and `git` installed and in your user's `PATH`.  

Next, make sure you have the clj-esptool code available on your machine.  Git/GitHub beginners may want to use the
following command to download the latest storm-starter code and change to the new directory that contains the downloaded
code.

    $ git clone git://github.com/jaejunh/clj-espertool.git && cd esptool 

### "clj-esptool" Overview

clj-esptool is command line tool for testing Esper and Event Processing Langague(EPL) Statements.  If this is your first time
working with "esper", please read these documents and example codes first:

1. [EsperTech](http://esper.codehaus.org):  Basic Esper's documents, especially "tutorial" first.

2. [Esper-4.10.0](http://esper.codehaus.org/esper-4.10.0/doc/reference/en-US/html_single/index.html#outputspec-simple): Example from Esper Document "Appendix A: Output Reference and Samples"


3. [Event Stream Processing Using Clojure and Esper Tutorial](http://patternhatch.com/2013/05/29/event-stream-processing-using-clojure-and-esper/):  Blog on How to Use Esper With Clojure [patternhatch.com](http://patternhatch.com/).  Disclaimer:  This tool is inspired by PatternHatch.com. 



After you have familiarized yourself with esper with clojure, take a look at the first example
[market-data.clj](market-data.clj) to see how "A.2 Output for Un-aggregated and Un-grouped Queries"
can be tested using clj-esptool.

---


## Using clj-esptool with Leiningen

### Install Leiningen

The clj-esptool uses [leiningen 2.0](http://leiningen.org). Install Install Leiningen by following the
[leiningen installation instructions](https://github.com/technomancy/leiningen).


### Install lein exec 

Although it's not neccessary, I recommend you to install [lein-exec](https://github.com/kumarshantanu/lein-exec)  to run example clojure script outside of REPL. 


---

## First Demo: "select * from LogDataEvent"


### ESPER and RDMBS Comparison

Esper CEP is event processing which is very similar to RDBMS system, where you need to specify 

1. "event type" 
2. the data of that "event type" 
3. and "EPL statement".  

If you already have used RDBMS and SQL, you can think of "event type" as "table or schema definition", the data as "row or record" in the table, and an EPL Statement as "SQL Statement (or maybe stored procedure in a sense that it is stored in the the Esper system once it is created).  



### Event Types and Data Publisher Are Already Supplied with Code (Just Read!)

In our first demo, I have already created LogDataEvent as 

```clojure 
;; 1. "event type"
(def log-data-event
        (new-event "LogDataEvent"
                { "ptime" :string
                  "server" :string
                  "program" :string
                  "pid" :int
                 "level" :string
                  "owner" :string
                  "message" :string }))
 
```

Also, I have created simple data generator of LogDataEvent in

```clojure
;; 2. the data publisher of "event type"

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
```


### Try Test Run an EPL Statement Dynamically with Lein REPL

Now let's try to supply 3. EPL Statement: "select * from LogDataEvent" in lein REPL.  



```shell
$ lein repl
```

```clojure
user.clj-esptool> (future-call log-data-publisher) ;; example zeromq publisher spitting "syslog" like output.

user.clj-esptool> (esp :help)
;; diplay help on available (esp :some-command)

user.clj-esptool> (esp :list-type) 
;; list registered event types.  You will see "LogDataEvent" I created for you.

user.clj-esptool> (esp :list) 
;; list registered EPL statements
user.clj-esptool> (esp :add "select * from LogDataEvent") 
;; Statement "s1" will be added as "select * from LogDataEvent" being ready to processed.


user.clj-esptool> (demo 5)
;; you will see 5 events fetched from log-data-publisher got processed.
;; To run more example, say 100000, (demo 100000).  
```



