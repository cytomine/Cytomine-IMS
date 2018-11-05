#!/bin/bash

git fetch --tags
export VERSION=$(cat application.properties | grep app.version)
export VERSION=${VERSION#app.version=}
export DEPLOY=false
if [[ ! $(git tag -l v$VERSION) ]]; then
    git config --local user.name "$(git log -1 --pretty=format:'%an')"
    git config --local user.email "$(git log -1 --pretty=format:'%ae')"
    git tag "v$VERSION"
    export DEPLOY=true

    ./grailsw rest-api-doc
    ./grailsw war
    cp restapidoc.json docker/
    cp IMS.war docker/
    docker build --build-arg RELEASE_PATH="." -t cytomineuliege/ims:latest -t cytomineuliege/ims:v$VERSION docker/
fi;