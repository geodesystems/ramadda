#!/bin/bash
MYDIR=`dirname $0`

set -e
set -o pipefail 2>/dev/null || : 


##In case this script was sources from another with . seesv.sh
##check if 
if [ ! -e "$MYDIR/lib/seesv.jar" ]; then
    MYDIR="${SEESV}"
fi

if [ ! -e "$MYDIR/lib/seesv.jar" ]; then
    printf "Error: cannot find $MYDIR/lib/seesv.jar\nIs the SEESV environment variable set?\n" >&2
    false
fi


if [ -z "$JAVA" ]; then
    JAVA=java
fi

## add the following for maxing out the memory
## -Xms16g -Xmx16g

${JAVA} -Djava.awt.headless=true -jar  ${MYDIR}/lib/seesv.jar "$@"

