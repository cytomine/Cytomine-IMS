#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Publish war ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"
ssh cytomine@192.168.122.16 "mkdir -p /home/cytomine/public_html/drupal7/dwnld/dev/releases/ims/$VERSION_NUMBER"
scp ./ci/IMS.war cytomine@192.168.122.16:/home/cytomine/public_html/drupal7/dwnld/dev/releases/ims/$VERSION_NUMBER/IMS.war

IMS_URL="https://cytomine.com/dwnld/dev/releases/ims/$VERSION_NUMBER/IMS.war"
echo $IMS_URL
echo $IMS_URL > ./ci/url