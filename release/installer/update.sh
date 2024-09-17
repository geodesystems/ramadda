#!/bin/sh


# This script updates your RAMADDA installation
# Run this script as sudo:
# sudo sh update.sh -dir <target dir> -dev (development release)

export MYDIR="$(cd "$(dirname "$0")" && pwd)"
. ${MYDIR}/lib.sh

usage() {
    printf "usage: \n\t-dir <ramadda install dir> (directory that holds ramaddaserver)\n\t-dev (install the development version)\n"
}


startstop() {
    run_service ${SERVICE_NAME} $1
}


export RAMADDA_INSTALL_DIR=/mnt/ramadda/ramaddainstall
if [ ! -d "${RAMADDA_INSTALL_DIR}" ]; then
    export PARENT_DIR=`dirname $MYDIR`
    export RAMADDA_INSTALL_DIR=${PARENT_DIR}/ramaddainstall
fi

if [ ! -d "${RAMADDA_INSTALL_DIR}" ]; then
    export PARENT_DIR=`pwd`
    export RAMADDA_INSTALL_DIR=${PARENT_DIR}/ramaddainstall
fi


while [ $# -gt 0 ]
do
    arg=$1
    case $arg in
        -dir)
	    shift
	    export RAMADDA_INSTALL_DIR=$1
	    shift
            ;;
        -dev)
	    shift
	    export DEV="TRUE"
	    ;;
	*)
	    echo "Unknown argument:$arg"
	    usage
	    exit 1
	    ;;
	esac
done

if [ ! -d "${RAMADDA_INSTALL_DIR}" ]; then
    echo "Error: RAMADDA install directory does not exist: ${RAMADDA_INSTALL_DIR}"
    usage
    exit
fi


#get the latest release
if [ -z "$DEV" ]; then
    echo "downloading ramaddaserver.zip"
    wget  --quiet -O ramaddaserver.zip https://ramadda.org/repository/release/latest/ramaddaserver.zip
    echo "DONE"
else
    echo "downloading development ramaddaserver.zip"
    wget  --quiet -O ramaddaserver.zip https://ramadda.org/repository/release/dev/ramaddaserver.zip
fi

#stop ramadda
echo "stopping RAMADDA"
startstop stop

#install the new ramadda
echo "removing old install ${RAMADDA_INSTALL_DIR}/ramaddaserver"
rm -r -f ${RAMADDA_INSTALL_DIR}/ramaddaserver

echo "installing ramaddaserver"
unzip -q ramaddaserver.zip -d ${RAMADDA_INSTALL_DIR}

#start ramadda
startstop start 


printf "RAMADDA has been updated and restarted. Check the log in:\n${RAMADDA_INSTALL_DIR}/ramadda.log\n"
