#!/bin/bash

echo "Removing existing bundles dir"
rm -rf config/bundles

mkdir config/bundles

echo "Copying built jars into local dir"
cp ../modules/reka-*/target/reka-*.jar config/bundles/
rm config/bundles/reka-chronicle*

cp ../main/reka-main/target/reka-main-*.jar reka-main.jar

echo "Making bundles.reka"
for b in `find config/bundles -type f -name '*.jar'`; do echo bundle bundles/`basename $b`; done > config/bundles.reka
