#!/bin/sh
mydir=`dirname $0`

${JAVA} -jar  ${mydir}/lib/csvutil.jar "$@"

