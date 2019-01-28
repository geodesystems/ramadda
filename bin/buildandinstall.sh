#!/bin/sh                                                                                                             
mydir=`dirname $0`

#This is used for building and restarting RAMADDA on geodesystems.com
#This assumes the directory structure:

#the github source tree
#/some/dir/source/ramadda  

#the  ramadda home dir
#/some/dir/repository 

#where the server is run from, holds logs, the compiled release, etc:
#/some/dir/runtime  

##This will do a git update, ramadda build, stop ramadda service, 
#install new server and plugins, service start ramadda

#The location of the source, runtime and ramadda home dirs
BASEDIR=${mydir}/../..

RUNTIME_DIR=${BASEDIR}/runtime
SOURCE=${BASEDIR}/source/ramadda
RAMADDA_HOME=${BASEDIR}/repository
SERVER_DIR=${RUNTIME_DIR}/ramaddaserver


DIST=${SOURCE}/dist

echo "Updating GIT and running build"
pushd ${SOURCE}
#svn update
git pull --no-edit origin master
ant -Dbuild.compiler=javac1.7

if [ ! -d "${DIST}/ramaddaserver" ]; then
    echo "Build seemed to fail. Exiting"
    exit
fi

echo "Stopping RAMADDA"
#Stop the server
sudo service ramadda stop

#Copy the dev plugins
sudo cp ${DIST}/plugins/devplugins/* ${RAMADDA_HOME}/plugins
sudo cp ${DIST}/plugins/projectplugins/* ${RAMADDA_HOME}/plugins

#Copy the environment
echo "Saving the .env"
cp -f ${SERVER_DIR}/ramaddaenv.sh ${RUNTIME_DIR}
mv ${SERVER_DIR} ${SERVER_DIR}_BAK

cp -r ${DIST}/ramaddaserver ${RUNTIME_DIR}
chmod 755 ${SERVER_DIR}/*.sh
cp ${RUNTIME_DIR}/ramaddaenv.sh ${SERVER_DIR}

echo "Starting RAMADDA"
sudo service ramadda start

rm -r ${SERVER_DIR}_BAK

popd

