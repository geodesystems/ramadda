#!/bin/sh
#the SEESV environment variable needs to point to the directory holding the seesv.sh script in RAMADDA's SeeSV  release
#
sh ${SEESV}/seesv.sh -insert "" site "\${file_shortname}" -change site "^.*/" "" -change site "- U\.S\..*" "" \
   -change site ".*\(RST" "RST"     -trim site -change site "\)$" "" \
   "-extractdate" "time" "year" -gt year 1984 \
   "-summary" "year" "original_value" "site,latitude,longitude" "average"  -print  "$1"

#sh ${SEESV}/seesv.sh "-extractdate" "time" "year" "-summary" "year" "original_value" "latitude,longitude" "average" "-increase" "original_value_average" "1"  -print  "$1"
