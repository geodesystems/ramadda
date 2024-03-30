#!/bin/sh
mydir=`dirname $0`

set -e
set -o pipefail

if [ -z "$JAVA" ]; then
    JAVA=java
fi

${JAVA} -Djava.awt.headless=true -jar  ${mydir}/lib/seesv.jar "$@"

