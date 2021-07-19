#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Publish docker ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")
file='./ci/url'
IMS_URL=$(<"$file")

docker build --rm -f scripts/docker/ims/Dockerfile --build-arg IMS_URL=$IMS_URL --build-arg VERSION_NUMBER=$VERSION_NUMBER -t  cytomine/ims:v$VERSION_NUMBER ./scripts/docker/ims

docker push cytomine/ims:v$VERSION_NUMBER

docker rmi cytomine/ims:v$VERSION_NUMBER
