#!/bin/sh

#
#This script installs RAMADDA on a Linux machine
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"
. "${MYDIR}/lib.sh"


usage() {
    echo "installer.sh -os redhat -y (assume yes installer) -help "
    exit
}


#
# parse the args
#
while [ $# != 0 ]
do
    case $1 in 
	-os)
	    shift
	    os=$1;
	    ;;
	-help)
	    usage
	    ;;
	-y)
	    promptUser=0
	    YUM_ARG=--assumeyes
	    ;;
	*)
	    echo "Unknown argument $1"
	    usage
	    ;;
    esac
    shift
done



do_main_install

exit
