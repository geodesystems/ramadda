#!/bin/sh

CWD=`pwd`

RAMADDA_DIR=`dirname $0`
RAMADDA_PARENT_DIR=`dirname $RAMADDA_DIR`

#RAMADDA home directory
if [ -z "$RAMADDA_HOME" ]; then
    RAMADDA_HOME=${HOME}/.ramadda
fi

#Port RAMADDA runs on
if [ -z "$RAMADDA_PORT" ]; then
    RAMADDA_PORT=8080
fi

#Java settings
if [ -z "$JAVA" ]; then
    JAVA=java
fi

if [ -z "$JAVA_MEMORY" ]; then
    JAVA_MEMORY=2056m
fi


if [ -z "$JAVA_PERMGEN" ]; then
    JAVA_PERMGEN=256m
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

##See if there is one in the current working directory
if test  -e ${CWD}/ramaddaenv.sh ; then 
    . ${CWD}/ramaddaenv.sh
fi


echo "Running RAMADDA: ${JAVA} -Xmx${JAVA_MEMORY} -XX:MaxPermSize=${JAVA_PERMGEN} -Dfile.encoding=utf-8 -jar ${RAMADDA_DIR}/lib/ramadda.jar -port ${RAMADDA_PORT} -Dramadda_home=${RAMADDA_HOME} $* "

## Add -Djava.awt.headless=true if there are problems running remotely on a Mac

${JAVA} -Xmx${JAVA_MEMORY} -XX:MaxPermSize=${JAVA_PERMGEN} -Dfile.encoding=utf-8 -jar ${RAMADDA_DIR}/lib/ramadda.jar -port ${RAMADDA_PORT} -Dramadda_home=${RAMADDA_HOME} $* 












