#!/bin/sh

JAVA=java


dirname=`dirname $0`

${JAVA} -Xmx512m  -jar ${dirname}/lib/ramaddaclient.jar "$@"








