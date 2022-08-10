#!/bin/sh
mydir=`dirname $0`

if [ -z "$JAVA" ]; then
    JAVA=java
fi

${JAVA} -jar  ${mydir}/lib/seesv.jar -s3 "$@"

