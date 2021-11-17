#!/bin/sh
mydir=`dirname $0`
source ${mydir}/init.sh

source=${datadir}/electionExpenditures.csv

#${csv} -c full_name -u 0 -p expenditures_final.csv | sort -u > names.csv
#exit




wget -O ${source} --post-data="exportType=Expenditure&electionID=20&committeeID=-1&filingDateStart=&filingDateStop=&transactionDateStart=&transactionDateStop=" https://election.bouldercolorado.gov/electionExpenditures.php 


${csv} -columns "committee,type,candidate,filingdate,amendeddate,officialfiling,transactiondate,lastname,firstname,street,city,state,zip,expenditure,purpose" \
       -concat firstname,lastname " " "Full Name" \
       -columnsafter firstname full_name \
       -change "regex:.*Date" ".*Invalid Date.*" "" \
       -normal full_name \
       -change full_name "file:${mydir}/expenditure_patterns.txt" "" \
       -columnsafter committee "full_name,expenditure,purpose" \
       -case full_name proper \
       -change "expenditure" "_dollar_" "" \
       -p ${source} > expenditures_final.csv

${csv} -db " table.id boulder_campaign_expenditures table.label {Boulder Campaign Expenditures} \
defaultOrder expenditure \
table.showEntryCreate false \
table.cansearch false table.canlist false table.format {MM/dd/yyyy} \
committee.type enumeration committee.cansearch true  committee.canlist true   \
type.type enumeration  type.cansearch true  type.canlist true   \
candidate.type enumeration candidate.cansearch true  candidate.canlist true   \
filing_date.type {date}  filing_date.cansearch true  filing_date.canlist true   \
amended_date.type {date} \
official_filing.type {enumeration}  official_filing.cansearch true  official_filing.canlist true   \
transaction_date.type {date} \
full_name.cansearch true  full_name.canlist true   \
street.cansearch true  street.canlist true   \
city.type {enumeration}  city.cansearch true  city.canlist true   \
state.type {enumeration}  state.cansearch true  state.canlist true   \
zip.type {string} \
expenditure.unit \$  expenditure.canlist true   \
expenditure.cansearch true  expenditure.canlist true   \
purpose.canlist true   \
purpose.cansearch true  purpose.canlist true   \
" \
       expenditures_final.csv > boulder_campaign_expendituresdb.xml


stage_local  expenditures_final.csv
release_plugin  boulder_campaign_expendituresdb.xml


${csv} -c full_name -u 0 -p expenditures_final.csv | sort -u > names.csv
exit
