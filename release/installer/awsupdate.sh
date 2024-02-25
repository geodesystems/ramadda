#!/bin/bash

#
#This reinstalls RAMADDA on AWS Linux. It assumes ramadda has been installed as a service
#Run this from the ramaddainstall directory, i.e., the directory that holds the
#ramaddaserver directory or pass the argument:
#-dir /path/to/ramaddainstall
#

#MYDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export MYDIR="$(cd "$(dirname "$0")" && pwd)"

startstop() {
    service ramadda $1
}

. "${MYDIR}/update.sh"

