#!/bin/sh
export csv=~/bin/csv.sh 

source=source/electionExpenditures.csv


#wget -O ${source} --post-data="exportType=Expenditure&electionID=20&committeeID=-1&filingDateStart=&filingDateStop=&transactionDateStart=&transactionDateStop=" https://election.bouldercolorado.gov/electionExpenditures.php 


${csv} -columns "committee,type,candidate,filingdate,amendeddate,officialfiling,transactiondate,lastname,firstname,street,city,state,zip,expenditure,purpose" \
       -concat firstname,lastname " " "Full Name" \
       -columnsafter firstname full_name \
       -change "regex:.*Date" ".*Invalid Date.*" "" \
       -change "expenditure" "_dollar_" "" \
       -change full_name "(?i)^wix$" "Wix.com" \
       -change full_name "(?i)Pure Buttons" "Purebuttons.com" \
       -change full_name "(?i)Google; LLC" "Google" \
       -change full_name "(?i)Squarespace; Inc." "Square Space" \
       -change full_name "Amazon.com Services LLC" "Amazon" \
-p ${source} > expenditures_final.csv


${csv} -db " table.id boulder_campaign_expenditures table.label {Boulder Campaign Expenditures} \
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
expenditure.cansearch true  expenditure.canlist true   \
purpose.cansearch true  purpose.canlist true   \
" \
       expenditures_final.csv > boulder_campaign_expendituresdb.xml


cp expenditures_final.csv ~/
cp boulder_campaign_expendituresdb.xml ~/.ramadda/plugins/
sh /Users/jeffmc/source/ramadda/bin/scpgeode.sh 50.112.99.202  boulder_campaign_expendituresdb.xml plugins
