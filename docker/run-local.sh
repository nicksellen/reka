#!/bin/bash

sudo \
  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/\
  java \
  -jar reka-main.jar \
  config/main.reka
