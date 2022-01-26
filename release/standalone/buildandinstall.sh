#
#This will do an svn update, ant, stop ramadda, install new server and plugins, start ramadda
#


BASEDIR=/home/ec2-user
RAMADDA_HOME=/mnt/ramadda/repository

SERVERDIR=${BASEDIR}/ramadda/ramaddaserver

SOURCE=${BASEDIR}/source/ramadda-code
DIST=${SOURCE}/dist

echo "Updating SVN and running build"
cd ${SOURCE}
svn update
ant


if [ ! -d "${DIST}/ramaddaserver" ]; then
    echo "Build seemed to fail. Exiting"
    exit
fi


echo "Stopping RAMADDA"
#Stop the server
sudo service ramadda stop

#Copy the dev plugins
sudo cp ${DIST}/plugins/devplugins/* ${RAMADDA_HOME}/plugins

echo "Saving the .env"
#Copy the environment
cp -f ${SERVERDIR}/ramaddaenv.sh ${BASEDIR}/ramadda
mv ${SERVERDIR} ${SERVERDIR}_BAK

cp -r ${DIST}/ramaddaserver ${BASEDIR}/ramadda
chmod 755 ${SERVERDIR}/*.sh
cp ${BASEDIR}/ramadda/ramaddaenv.sh ${SERVERDIR}

echo "Starting RAMADDA"
sudo service ramadda start

rm -r ${SERVERDIR}_BAK


