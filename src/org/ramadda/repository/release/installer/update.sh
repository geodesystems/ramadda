#!/bin/sh

#
# This script updates your RAMADDA installation
# Get the latest version of the ramaddaserver.zip from 
# http://geodesystems.com/repository/alias/release
# and copy it into the ramaddainstall directory 
# Run this script with the full path, e.g.:
# sudo sh /home/ec2-user/ramaddainstaller/update.sh
#

serviceName="ramadda"
installerDir=`dirname $0`
parentDir=`dirname $installerDir`
ramaddaDir=${parentDir}/${serviceName}
serverDir=$ramaddaDir/ramaddaserver

echo "stopping ${serviceName}";
service ${serviceName} stop;
echo "Saving ${serverDir}/ramaddaenv.sh"
cp ${serverDir}/ramaddaenv.sh  ${installerDir}
echo "Nuking old install: ${serverDir}"
rm -r -f ${serverDir};
echo "Unzipping: ${installerDir}/ramaddaserver.zip"
unzip -d ${ramaddaDir} -o ${installerDir}/ramaddaserver.zip >/dev/null
echo "Copying back: ${installerDir}/ramaddaenv.sh"
mv ${installerDir}/ramaddaenv.sh ${serverDir}
echo "starting ${serviceName}";
service ${serviceName} start;

printf "RAMADDA has been updated and restarted. Check the log in:\n${ramaddaDir}/ramadda.log\n"
