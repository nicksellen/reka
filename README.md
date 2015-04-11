# reka

Welcome to reka. You can find out a little more at [reka.io](https://reka.io). It's not suitable for use in production right now.

## First things first

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
make clean run
````
