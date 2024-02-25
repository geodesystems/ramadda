#!/bin/sh


# This script updates your RAMADDA installation
# Run this script as sudo:
# sudo sh update.sh -dir <target dir> -dev (development release)

usage() {
    printf "usage: \n\t-dir <target dir> (directory that holds ramaddaserver)\n\t-dev (install the development version)\n"
}

export INSTALLDIR=/mnt/ramadda/ramaddainstall
if [ ! -d "${INSTALLDIR}" ]; then
    export INSTALLDIR=.
fi

while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -dir)
	    shift
	    export INSTALLDIR=$1
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

if [ ! -d "${INSTALLDIR}" ]; then
    echo "Error: RAMADDA install directory does not exist:${INSTALLDIR}"
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
startstop stop

#install the new ramadda
rm -r -f ${INSTALLDIR}/ramaddaserver
unzip -q ramaddaserver.zip -d ${INSTALLDIR}

#start ramadda
startstop start 


printf "RAMADDA has been updated and restarted. Check the log in:\n${INSTALLDIR}/ramadda.log\n"
