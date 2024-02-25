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
    wget  -O ramaddaserver.zip https://ramadda.org/repository/release/latest/ramaddaserver.zip
else
    echo "downloading development ramaddaserver.zip"
    wget  -O ramaddaserver.zip https://ramadda.org/repository/release/dev/ramaddaserver.zip
fi

#stop ramadda
startstop stop


#install the new ramadda
rm -r -f ${INSTALLDIR}/ramaddaserver
unzip ramaddaserver.zip -d ${INSTALLDIR}

#start ramadda
startstop start 


printf "RAMADDA has been updated and restarted. Check the log in:\n${INSTALLDIR}/ramadda.log\n"
