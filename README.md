# reka

[![Join the chat at https://gitter.im/nicksellen/reka](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nicksellen/reka?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Welcome to reka. You can find out a little more at [reka.io](https://reka.io).

* runs on JVM, written in Java 8
* build applications as a DAG of operations
* simple DSL for constructing DAG
* client/server architecture, so can deploy to remote server - [client](https://github.com/nicksellen/reka-cli) part written in golang
* tiny core (519kb jar), everything else implemented in external modules
* fully dogfood'ed - provisioning API and admin UI implemented with reka
* no downtime redeploys for network protocols - [web]socket connections can even be kept open
* available modules in various states of completeness for:
  * network: http, websockets, tcp sockets, ssl
  * email: smtp client/server, imap
  * databases: h2, postgres
  * languages: clojure, ruby (jruby), javascript (nashorn)
  * templating: markdown, mustache, jade
  * other: ssh, child process, irc, jsx
* not suitable for use in production right now.

I'm looking for feedback, inspiration, use cases, thoughts, insults, and cake.

## How to use

There is a [Getting Started](https://reka.io/getting-started) guide that will, well, get you started.

## How to build

Prequisites:

* java >=8
* maven >=3

If you need to specify special environment, write an env.make file, mine looks like this:

```
export JAVA_HOME=/usr/lib/jvm/java-8-oracle/
export REKA_ENV=development
````

## Build reka-server.tar.gz

````
make
````

## Run locally

````
make run
````

## Roadmap and limitations

In the future it might do things like:

* more modules: message queues, more databases, elasticsearch
* modules written in non-JVM languages
* live module reloading via OSGi
* transparent distributed deployment - fundemental architecture totally supports this
* "multi execution" graph architectures - currently each operation is executed once or not at all for a given trigger
* include browser javascript implemention to seamless execute in browser or on backend
* "intellisense"-like service for editors/IDEs
* allow provisioning/configuring of kubernetes/docker instances

Current limitations:

* only single server deployment
* no type verification of data flowing through execution graph
* not good enough solution for error handling yet
* somewhat verbose/complicated to write simple modules in Java
* need documentation on available modules/operations/configuration
