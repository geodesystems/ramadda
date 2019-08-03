#!/bin/sh

mydir=`dirname $0`

#This script builds the the minified versions of the javascript and css then
#does a git commit/push of the source tree then rebuilds RAMADDA on AWS
#It uses any argument as the git commit message
RAMADDA_SRC=${mydir}/../
#/Users/jeffmc/source/ramadda


text="update"
if [  "$1" ]; then
    text="$1"
fi

target="release"
if [  "$2" ]; then
    target="$2"
fi

pushd ${RAMADDA_SRC}
echo "making htdocs";
/Users/jeffmc/software/apache-ant-1.10.6/bin/ant -buildfile ${RAMADDA_SRC}/src/org/ramadda/repository/build.xml htdocs
echo "commiting";
git commit -m "${text}" -a
echo "pushing";
git push
echo "building";
sh ${mydir}/makerelease.sh ${GEODESYSTEMS_IP} ${target}
popd
