#!/bin/bash

set -e
##Ignore and hide any  error in case this is running in a shell that does not have pipefail
set -o pipefail 2>/dev/null || : 

if [ ! -d "images" ]; then
    mkdir images
fi

if [ ! -e "${SEESV}/seesv.sh" ]; then
    printf "Error: cannot find ${SEESV}/seesv.sh\nIs the SEESV environment variable set?\n" >&2
fi


seesv() {
    . ${SEESV}/seesv.sh "$@"
}



##The argument is the copied text from the NAMUS search page
grep "cell-contents\">MP" $1  > cases.csv
seesv -lines -striptags 0 -change 0 MP "" -p cases.csv > ids.csv
java org.ramadda.projects.missing.NamusConverter ids.csv



