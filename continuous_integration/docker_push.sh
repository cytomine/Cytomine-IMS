#!/bin/bash

docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
docker push cytomineuliege/ims:v$VERSION
#docker push cytomineuliege/ims