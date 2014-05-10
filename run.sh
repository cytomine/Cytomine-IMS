#!/bin/bash
gvm use grails 2.3.8
if [[ "$OSTYPE" == "darwin"* ]]; then
PROGRAM="./fcgi-bin/os_x/iipsrv.fcgi --bind 127.0.0.1:9000"
elif [[ "$OSTYPE" == "linux-gnu" ]]; then
PROGRAM="./fcgi-bin/linux/iipsrv.fcgi --bind 127.0.0.1:9000"
fi
export VERBOSITY=6
export MAX_IMAGE_CACHE_SIZE=0
$PROGRAM&
PID=$!

grails -Dserver.port=9080 run-app

if [[ $? -gt 128 ]]
then
	echo "kill $PID"
    kill $PID
fi