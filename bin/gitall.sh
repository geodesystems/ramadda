#!/bin/sh

mydir=`dirname $0`

#This script builds the the minified versions of the javascript and css then
#does a git commit/push of the source tree then rebuilds RAMADDA on AWS
#It uses any argument as the git commit message
RAMADDA_SRC=${mydir}/../
#/Users/jeffmc/source/ramadda



sh ${mydir}/doall.sh
echo "building";
sh ${mydir}/makerelease.sh ${GEODESYSTEMS_IP} ${target}
popd
