# Esper Command Line Tool and Example 

Learn to use Esper!

---

Table of Contents

* Getting Started 
* Rationale Behind this clj-esptool
* How to Install
* First Run 
* Test Examples from Esper Document
* Commands
* Disclaimer  

---

# Getting Started

## Prerequisites

First, you need `java` and `git` installed and in your user's `PATH`.  

Next, make sure you have the clj-esptool code available on your machine.  Git/GitHub beginners may want to use the
following command to download the latest storm-starter code and change to the new directory that contains the downloaded
code.

    $ git clone git://github.com/jaejunh/clj-espertool.git && cd esptool 

## clj-esptool overview

clj-esptool is command line tool for testing Esper and Event Processing Langague(EPL) Statements.  If this is your first time
working with "esper", please read these documents and example codes first:

1. [EsperTech](http://esper.codehaus.org):  Basic Esper's documents, especially "tutorial" first.

2. [Esper-4.10.0](http://esper.codehaus.org/esper-4.10.0/doc/reference/en-US/html_single/index.html#outputspec-simple): Example from Esper Document "Appendix A: Output Reference and Samples"


3. [Event Stream Processing Using Clojure and Esper Tutorial](http://patternhatch.com/2013/05/29/event-stream-processing-using-clojure-and-esper/):  Blog on How to Use Esper With Clojure [patternhatch.com](http://patternhatch.com/).  Disclaimer:  This tool is inspired by PatternHatch.com. 



After you have familiarized yourself with esper with clojure, take a look at the first example
[market-data.clj](market-data.clj) to see how "A.2 Output for Un-aggregated and Un-grouped Queries"
can be tested using clj-esptool.


# Using clj-esptool with Leiningen

## Install Leiningen

The clj-esptool uses [leiningen 2.0](http://leiningen.org). Install Install Leiningen by following the
[leiningen installation instructions](https://github.com/technomancy/leiningen).


## Install lein exec 

Although it's not neccessary, I recommend you to install [lein-exec](https://github.com/kumarshantanu/lein-exec)  to run example clojure script outside of REPL. 


## Running "first demo: select * from LogDataEvent" with Leiningen


```shell
$ lein repl
```

```clojure
user.clj-esptool> (future-call log-data-publisher) ;; example zeromq publisher spitting "syslog" like output.

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



