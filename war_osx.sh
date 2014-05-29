#!/bin/bash
mkdir tmp
cp lib/openslide.jar tmp/openslide.jar
cp natives/openslide/osx/openslide.jar lib/openslide.jar
grails war
cp tmp/openslide.jar lib/openslide.jar
rm -r tmp

