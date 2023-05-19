#!/bin/sh


# This script updates your RAMADDA installation
# Run this script as sudo:
# sudo sh update.sh


INSTALLER_DIR="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
source "${INSTALLER_DIR}/lib.sh"


echo "fetching the latest RAMADDA from ${RAMADDA_DOWNLOAD}"
wget -O ${INSTALLER_DIR}/ramaddaserver.zip "${RAMADDA_DOWNLOAD}"

echo "stopping ${SERVICE_NAME}";
service ${SERVICE_NAME} stop;

echo "Saving ${RAMADDA_SERVER_DIR}/ramaddaenv.sh"
cp ${RUNTIME_DIR}/ramaddaenv.sh  ${INSTALLER_DIR}

#echo "deleting the old install: ${RAMADDA_SERVER_DIR}"
#rm -r -f ${RAMADDA_SERVER_DIR};

echo "Unzipping: ${INSTALLER_DIR}/ramaddaserver.zip"
unzip -d ${RUNTIME_DIR} -o ${INSTALLER_DIR}/ramaddaserver.zip >/dev/null

#echo "Copying back: ${INSTALLER_DIR}/ramaddaenv.sh"
#mv ${INSTALLER_DIR}/ramaddaenv.sh ${serverDir}

echo "starting ${SERVICE_NAME}";
service ${SERVICE_NAME} start;

printf "RAMADDA has been updated and restarted. Check the log in:\n${RUNTIME_DIR}/ramadda.log\n"
