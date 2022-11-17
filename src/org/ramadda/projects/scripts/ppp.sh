#!/bin/sh
#This scripts converts the PPP data from https://data.sba.gov/dataset/ppp-foia into a
#RAMADDA DB file
#
mydir=`dirname $0`
~/bin/csv.sh  -cleaninput -progress 5000 -sample 0.001 \
      -firstcolumns "borrower_.*,CurrentApprovalAmount" \
      -notcolumns ".*PROCEED.*" \
      -case "BorrowerCity,ServicingLenderCity,ProjectCity,Ethnicity,OriginatingLenderCity" proper \
      -denormalize ${mydir}/naics.csv 0 1 naicscode "" replace \
      -p "$@" > processedppp.csv
~/bin/csv.sh  -cleaninput -progress 5000 -db "file:${mydir}/ppp.properties" processedppp.csv > pppdb.xml
