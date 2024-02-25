#!/bin/sh


# This script updates your RAMADDA installation
# Run this script as sudo:
# sudo sh update.sh


export INSTALLDIR=.
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
	    printf "usage: \n\t-dir <target dir>"
	    exit 1
	    ;;
	esac
done

if [ ! -d "${INSTALLDIR}/ramaddaserver" ]; then
    echo "Error: RAMADDA install directory does not exist: ${INSTALLDIR}"
    printf "usage: \n\t-dir <target dir>"
    exit
fi


#get the latest release
if [ -z "$DEV" ]; then
    echo "downloading ramaddaserver.zip"
    wget  -O ramaddaserver.zip https://ramadda.org/repository/entry/get/ramaddaserver.zip?entryid=synth%3A498644e1-20e4-426a-838b-65cffe8bd66f%3AL3JhbWFkZGFzZXJ2ZXIuemlw
else
    echo "downloading development ramaddaserver.zip"
    wget  -O ramaddaserver.zip https://ramadda.org/repository/entry/get/ramaddaserver.zip?entryid=synth%3Ae67adef4-f28d-4818-ab2b-066e526696ec%3AL3JhbWFkZGFzZXJ2ZXIuemlw
fi

#stop ramadda
startstop stop


#install the new ramadda
rm -r -f ${INSTALLDIR}/ramaddaserver
unzip ramaddaserver.zip -d ${INSTALLDIR}

#start ramadda
startstop start 


printf "RAMADDA has been updated and restarted. Check the log in:\n${INSTALLDIR}/ramadda.log\n"
