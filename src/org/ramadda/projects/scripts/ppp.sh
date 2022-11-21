#!/bin/sh
#This scripts converts the PPP data from https://data.sba.gov/dataset/ppp-foia into a
#RAMADDA DB file
#
mydir=`dirname $0`
source "${mydir}/init.sh"
#-sample 0.001 \
seesv  -cleaninput -progress 5000 \
	  -firstcolumns "borrower_.*,CurrentApprovalAmount" \
	  -change borrowername "  +" " " \
	  -notcolumns ".*PROCEED.*,originating.*,sbaofficecode,loannumber,processingmethod,servicinglenderlocationid,sbaofficecode,originatinglenderlocationid,HubzoneIndicator,lmiindicator,businessagedescription" \
	  -case "BorrowerCity,ServicingLenderCity,ProjectCity,Ethnicity" proper \
	  -denormalize ${mydir}/naics.csv 0 1 naicscode "" replace \
	  -p "$@" 

