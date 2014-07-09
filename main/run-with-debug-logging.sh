#!/bin/bash

java -Dlog4j.configurationFile=reka-main-uberjar/log4j2-debug.xml -jar reka-main-uberjar/target/reka-main-uberjar-0.1.0.jar "$@"
