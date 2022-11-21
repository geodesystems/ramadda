#!/bin/sh
export mydir=`dirname $0`
#Exit on error
set -e

if [  -e "${SEESV_HOME}" ]
then
    export seesv=${SEESV_HOME}/seesv.sh
else
    export seesv=~/bin/seesv.sh
fi


seesv() {
    sh $seesv "$@"
}
