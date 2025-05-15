#!/bin/sh

export MYDIR="$(cd "$(dirname "$0")" && pwd)"

#RAMADDA install directory
if [ -z "$RAMADDA_DIR" ]; then
    RAMADDA_DIR="$MYDIR"
fi


RAMADDA_PARENT_DIR=`dirname $RAMADDA_DIR`

#RAMADDA home directory
if [ -z "$RAMADDA_HOME" ]; then
    RAMADDA_HOME=${HOME}/.ramadda
fi

#Port RAMADDA runs on
if [ -z "$RAMADDA_PORT" ]; then
    RAMADDA_PORT=8080
fi

#SSL Port RAMADDA runs on
if [ -z "$RAMADDA_SSL_PORT" ]; then
    RAMADDA_PORT=443
fi

#Java settings
if [ -z "$JAVA" ]; then
    JAVA=java
fi

if [ -z "$JAVA_MEMORY" ]; then
    JAVA_MEMORY=2056m
fi


##See if there is one in the release dir
RAMADDA_ENV_FILE=${RAMADDA_DIR}/ramaddaenv.sh
if test -e  ${RAMADDA_ENV_FILE} ; then 
    . ${RAMADDA_ENV_FILE}
fi

##See if there is one in the parent dir
RAMADDA_ENV_FILE=${RAMADDA_PARENT_DIR}/ramaddaenv.sh
if test -e  ${RAMADDA_ENV_FILE} ; then 
    echo "RAMADDA initializing with: ${RAMADDA_ENV_FILE}"
    . ${RAMADDA_ENV_FILE}
fi

## Add -Djava.awt.headless=true if there are problems running remotely on a Mac

echo "Running RAMADDA: ${JAVA} -Xmx${JAVA_MEMORY}   -DLC_CTYPE=UTF-8 -Dsun.jnu.encoding=UTF-8  -Dfile.encoding=utf-8 -jar ${RAMADDA_DIR}/lib/ramadda.jar -port ${RAMADDA_PORT} -sslport ${RAMADDA_SSL_PORT} -Dramadda_home=${RAMADDA_HOME} $* " > ${RAMADDA_PARENT_DIR}/service.out

${JAVA} -Xmx${JAVA_MEMORY}   -DLC_CTYPE=UTF-8 -Dsun.jnu.encoding=UTF-8  -Dfile.encoding=utf-8 -jar ${RAMADDA_DIR}/lib/ramadda.jar -port ${RAMADDA_PORT} -sslport ${RAMADDA_SSL_PORT}  -Dramadda_home=${RAMADDA_HOME} $* 












