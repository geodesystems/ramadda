#!/bin/sh

#
#This reinstalls RAMADDA on AWS Linux. It assumes ramadda has been installed as a service
#Run this from the ramaddainstall directory, i.e., the directory that holds the
#ramaddaserver directory
#

INSTALLDIR=.
while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -dir)
	    shift
	    INSTALLDIR=$1
	    shift
            ;;
	*)
	    echo "Unknown argument:$arg"
	    echo "usage: \n\t-dir <target dir>"
	    exit 1
	    ;;
	esac
done


#get the latest release
wget  -O ramaddaserver.zip https://ramadda.org/repository/entry/get/ramaddaserver.zip?entryid=synth%3A498644e1-20e4-426a-838b-65cffe8bd66f%3AL3JhbWFkZGFzZXJ2ZXIuemlw

#stop ramadda
service ramadda stop 

#install the new ramadda
rm -r -f ${INSTALLDIR}/ramaddaserver
unzip ramaddaserver.zip -d ${INSTALLDIR}

#start ramadda
service ramadda start 


