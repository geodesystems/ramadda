#!/bin/sh
#This scripts converts the PPP data from https://data.sba.gov/dataset/ppp-foia into a
#RAMADDA DB file
#
mydir=`dirname $0`
~/bin/csv.sh  -cleaninput -progress 5000 -db "file:${mydir}/ppp.properties" "$@" > pppdb.xml
