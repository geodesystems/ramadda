#!/bin/bash

#
#the SEESV environment variable should be set and needs to point to the directory
#holding the seesv.sh script in RAMADDA's SeeSV  release 
#

set -e
##Ignore and hide any  error in case this is running in a shell that does not have pipefail
set -o pipefail 2>/dev/null || : 

if [ ! -e "${SEESV}/seesv.sh" ]; then
    printf "Error: cannot find ${SEESV}/seesv.sh\nIs the SEESV environment variable set?\n" >&2
fi


seesv() {
    . ${SEESV}/seesv.sh "$@"
}

seesv -tojson "" loggers.csv > loggers.json
