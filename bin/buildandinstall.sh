#!/bin/sh                                                                                                             
MYDIR=`dirname $0`

#This is used for building and restarting RAMADDA on geodesystems.com
#This assumes the directory structure:


export RAMADDA_BASE=/mnt/ramadda
export ANT_OPTS="-Xmx1000m"
export ANT_HOME="${RAMADDA_BASE}/home/ant"
export PATH=${ANT_HOME}/bin:$PATH:$HOME/bin


#the github source tree
#/some/dir/source/ramadda  

#the  ramadda home dir
#/some/dir/repository 
target="release"
if [  "$1" ]; then
    target="$1"
fi


#where the server is run from, holds logs, the compiled release, etc:
#/some/dir/runtime  

##This will do a git update, ramadda build, stop ramadda service, 
#install new server and plugins, service start ramadda

#The location of the source, runtime and ramadda home dirs

PARENT=`dirname $MYDIR`
GRANDPARENT=`dirname $PARENT`
GREATPARENT=`dirname $GRANDPARENT`
BASEDIR=${GREATPARENT}
echo "BASE=${BASEDIR}"


RUNTIME_DIR=${BASEDIR}/runtime
SOURCE=${BASEDIR}/source/ramadda
RAMADDA_BIN=${BASEDIR}/source/ramadda/bin
RAMADDA_HOME=${BASEDIR}/repository
SERVER_DIR=${RUNTIME_DIR}/ramaddaserver

echo "Doing the dev release"
sh ${RAMADDA_BIN}/devrelease.sh
exit


DIST=${SOURCE}/dist

echo "Updating GIT and running build"
pushd ${SOURCE}
#svn update
git pull --no-edit origin master
ant  -Dbuild.compiler=javac1.7 -S ${target}

if [ ! -d "${DIST}/ramaddaserver" ]; then
    echo "Build seemed to fail. No ${DIST}/ramaddaserver dir. Exiting"
    exit
fi

echo "Stopping RAMADDA"
#Stop the server
sudo service ramadda stop

#Copy the dev plugins
sudo cp ${DIST}/plugins/devplugins/* ${RAMADDA_HOME}/plugins
sudo cp ${DIST}/plugins/projectplugins/* ${RAMADDA_HOME}/plugins

#Copy the environment
RUNTIME_ENV="${RUNTIME_DIR}/ramaddaenv.sh"
if [ ! -f "${RUNTIME_ENV}" ]; then
    echo "Copying a local ramaddaenv.sh"
    cp -f ${SERVER_DIR}/ramaddaenv.sh ${RUNTIME_ENV}
fi

mv ${SERVER_DIR} ${SERVER_DIR}_BAK
cp -r ${DIST}/ramaddaserver ${RUNTIME_DIR}
chmod 755 ${SERVER_DIR}/*.sh
#cp ${RUNTIME_DIR}/ramaddaenv.sh ${SERVER_DIR}

echo "Starting RAMADDA"
sudo service ramadda start

rm -r ${SERVER_DIR}_BAK
popd

#And do the dev release
echo "Doing the dev release"
sh ${RAMADDA_BIN}/devrelease.sh
