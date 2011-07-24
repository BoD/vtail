#!/bin/sh
VERSION=1.00

cd ..
mvn clean assembly:assembly
rm -rf dist
mkdir -p dist/vtail
cp target/vtail-$VERSION-with-deps.jar dist/vtail
cp etc/vtail dist/vtail
cp -r etc dist/vtail/etc
rm dist/vtail/etc/dist.sh
rm dist/vtail/etc/vtail
cd dist
tar cvfz vtail-$VERSION.tgz vtail
