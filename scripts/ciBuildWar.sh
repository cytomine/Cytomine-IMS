#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Build war ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"

docker build --rm -f scripts/docker/Dockerfile-war.build --build-arg VERSION_NUMBER=$VERSION_NUMBER -t  cytomine/cytomine-ims-war .

containerId=$(docker create cytomine/cytomine-ims-war )
#docker network connect scripts_default $containerId
docker start -ai  $containerId

#docker cp $containerId:/app/target ./ci
docker cp $containerId:/app/IMS.war ./ci

docker rm $containerId
docker rmi cytomine/cytomine-ims-war