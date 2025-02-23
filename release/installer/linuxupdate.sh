#!/bin/sh

#
#This reinstalls RAMADDA on AWS Linux. It assumes ramadda has been installed as a service
#Run this from the ramaddainstall directory, i.e., the directory that holds the
#ramaddaserver directory or pass the argument:
#-dir /path/to/ramaddainstall
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"

startstop() {
    systemctl $1 ramadda
}

. "${MYDIR}/update.sh"

