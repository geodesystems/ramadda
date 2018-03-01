#!/bin/sh

JAVA=java


dirname=`dirname $0`

source "$dirname/ramaddaenv.sh"

${JAVA} -Xmx512m  -jar ${dirname}/lib/ramaddasearch.jar "$@"








