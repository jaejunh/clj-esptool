# Clojure Esper Command Line Tool

Welcome.  I created this sample project to learn Esper with Clojure. 
Esper in nature is java embedded library and CEP environment where 
it's stand-out GPL software with commercial maturity to handle real 
time event processing.  

Yet ESPER libary really didn't have simple command line tool (such as SQLPlus in Oracle RDBMS)
and it was difficult for me to see how EPL interacts with
Esper and etc. I would like my friends at my company [embian](http://www.embian.com) to learn
this CEP concept quickly by trying out EPL examples, not by hacking time consuming 
embedded java client and server coding.
 

After I have learned enough how to juggle Esper with Clojure with the tool,
I decided to make it available to share with people, though premature project
to help people like us.


 

## Table of Contents

* Getting Started  

* Using clj-esptool with Leiningen 

* First Demo: "select * from LogDataEvent" 

* Command Examples 




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
[market-data.clj](test-script/market-data.clj) to see how "A.2 Output for Un-aggregated and Un-grouped Queries"
can be tested using clj-esptool.




## Using clj-esptool with Leiningen

### Install Leiningen

The clj-esptool uses [leiningen 2.0](http://leiningen.org). Install Install Leiningen by following the
[leiningen installation instructions](https://github.com/technomancy/leiningen).


### Install lein exec 

Although it's not neccessary, it's very handy if you can run clojure script outside of REPL.  I recommend you to install [lein-exec](https://github.com/kumarshantanu/lein-exec)  to run the example script from your shell. 




## First Demo: "select * from LogDataEvent"


### ESPER and RDMBS Comparison

Esper CEP is event processing which is very similar to RDBMS's data repository model, where you need to specify 

1. "event type" 
2. data of the "event type" 
3. and "EPL statement".  

If you already have used RDBMS and SQL, you can think of "event type" as "table or schema definition", the data as "row or record" in the table, and an EPL Statement as a "SQL Statement (or maybe a stored procedure in a sense that it is stored in the the Esper system once it is created).  



### Example Event Type and Data Publisher Supplied with Code 

In our first demo, I have already created an example event type,  "LogDataEvent" as 

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


### Try an EPL Statement Dynamically with Lein REPL

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


## Command Examples

You can always "(esp)" or "(esp :help)" in REPL to see brief example of command.
Also, try take a look at "market-data.clj" example usages.

Here is the out of (esp :help).


### clj-esptool commands

```clojure
clj-esptool.core=> (esp)

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
	(esp :add-type "MarketData" {"symbol" :string, "price": double, "volume": int, "omit" :string})
	(esp :remove-type "MarketData") 
	(esp :remove-type :all) (esp :remove-type "all")
	(esp :display-type "MarketData")
	
	(esp :list)
	(esp :add "select * from MarketData")
	(esp :remove "s01")
	(esp :remove :all) (esp :remove "all")
	(esp :display "s01")
	(esp :stop  :all) (esp :stop "all")
	(esp :start :all) (esp :start "all")


	(esp :shoot-init)
	(esp :shoot 
		0.2   				;; shoot delay second
	    	"Market-data" 		;; event-type
		["symbol", "volume", "price", "omit" ]	;; event properties
		["IBMi", 100, 25.0, "some-string"])  ;; event data 

	(esp :list-service)
	(esp :add-service "test")
	(esp :use-service)
	(esp :use-service "test")
	(esp :use-service)
	(esp :remove-service "test")
```

### (optional configuration) How to run clojure test script

lein has a support for running clojure as script just like shell script.  In order to run
clojure script,
you need to configure lein exec as mentioned earlier, and I have created simple "lein-x" shell
to invoke "lein exec".  

Let's say you have tested series of clj-esptool commands and you want to rerun that
time to time.  Best way is to save those commands in a file, and run it when necessary.

To show you how that can be achieved easily, I have created an example script

"market-data.clj".  


In order to run this,  let's copy lein-x into your ~/bin directory

```shell 
mkdir -p ~/bin; cp lein-x  ~/bin 

which lein-x  #to check lein-x is in your path.  If not, logout and login.

lein-x test-script/market-data.clj "(mytest)"
```

First argument of lein-x is script filename to load, and the second argument is optional
in-line clojure code. (in our case, the test function name to be executed)








