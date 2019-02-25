#!/bin/sh

#This script does a git commit/push of the source tree then rebuilds RAMADDA on AWS
#It uses any argument as the git commit message

mydir=`dirname $0`
RAMADDA_SRC=${mydir}/../


text="update"
if [  "$1" ]; then
    text="$1"
fi

target="release"
if [  "$2" ]; then
    target="$2"
fi


pushd ${RAMADDA_SRC}
echo "commiting";
git commit -m "${text}" -a
echo "pushing";
git push
echo "building";
sh ${mydir}/makerelease.sh ${GEODESYSTEMS_IP} ${target}
popd
