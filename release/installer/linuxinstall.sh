#!/bin/sh

#
#This script installs RAMADDA on a Linux machine
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"
. "${MYDIR}/lib.sh"


do_main_install

exit
