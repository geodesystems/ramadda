#!/bin/sh
export csv=~/bin/csv.sh 


#wget -O source/new.csv --post-data="exportType=Contribution&electionID=20&committeeID=-1&filingDateStart=&filingDateStop=&transactionDateStart=&transactionDateStop=" https://election.bouldercolorado.gov/electionContributions.php


dots=2000
do_convert() {
    ${csv} -set Match 0 MatchAmount -set CommitteeNum 0 CommitteeNumber  \
	   -change filingdate,amendeddate,transactiondate "(....)/(..)/(..).*" "\$1/\$2/\$3" \
	   -case  FromCandidate lower \
	   -case  anonymous lower \
	   -p source/old.csv > oldtmp.csv
    ${csv} -notcolumns "YTDAmount,AmendsContributionID,ContributionID" \
	   -change filingdate,amendeddate,transactiondate "(..)/(..)/(....)" "\$3/\$1/\$2" \
	   -case  FromCandidate lower \
	   -case  anonymous lower \
	   -p source/new.csv > newtmp.csv
}

#do_convert
#exit


do_contributions() {
    infile=$1
    outfile=$2
    ${csv} -notcolumns "0,committeenumber" \
	   -set filingdate 0 "Filing Date" \
	   -set transactiondate  0 transaction_date \
	   -set lastname  0 last_name \
	   -set firstname  0 first_name \
	   -if -same first_name last_name  -change last_name ".*" "" -endif \
	   -extract filing_date "(\\d\\d\\d\\d)" none election_year \
	   -columnsafter transaction_date election_year \
	   -concat "first_name,last_name" " " "Full Name"  \
	   -case full_name upper -trim full_name   -change full_name "  +" " " \
	   -change full_name ".*COCA-COLA.*" "THE COCA-COLA COMPANY" \
	   -change full_name "INDIAN PEAKS GROUP SIERRA CLUB" "SIERRA CLUB INDIAN PEAKS GROUP" \
	   -change full_name "HOUSING HELPERS OF BOULDER, LLC" "HOUSING HELPERS OF BOULDER" \
	   -change full_name "BOULDER AREA REALTORS ASSN" "BOULDER AREA REALTORS" \
	   -change full_name "SIERRA CLUB IPG" "SIERRA CLUB INDIAN PEAKS GROUP" \
	   -change full_name ".*NEW ERA.*" "NEW ERA" \
	   -change full_name "PLAN.*BOULDER.*" "PLAN BOULDER" \
	   -change full_name "VOTERS AGAINST XCEL BUYING ELECTIONS" "XXXX" \
	   -change full_name ".*XCEL.*" "XCEL ENERGY" \
	   -change full_name "XXXX" "VOTERS AGAINST XCEL BUYING ELECTIONS" \
	   -change full_name "OPEN BOULDER.*" "OPEN BOULDER" \
	   -change full_name ".*AMERICAN BEVERAGE ASSOCIATION.*" "AMERICAN BEVERAGE ASSOCIATION" \
	   -columnsbefore last_name full_name \
	   -set amendeddate	 0 amended_date	   \
	   -change contribution,fromcandidate,matchamount "_dollar_" "" \
	   -change contribution,fromcandidate,matchamount "_leftparen_(.*)_rightparen_" "-\$1" \
	   -change contribution,fromcandidate,matchamount "," "" \
	   -change Contribution "_dollar_" "" \
	   -p ${infile} > ${outfile}
}

echo "converting"
do_convert
echo "making old"
#do_contributions oldtmp.csv    contributions_old.csv
echo "making new"
do_contributions newtmp.csv    contributions_new.csv

cp contributions_old.csv contributions_final.csv
tail -n+2 contributions_new.csv >>contributions_final.csv
echo "making db"
${csv} -db " table.id boulder_campaign_contributions table.label {Boulder Campaign Contributions} \
table.include file:source/include.txt \
committee.numberOfSearchWidgets 5 \
committee.type enumeration type.type enumeration candidate.type enumeration \
official_filing.type enumeration \
from_candidate.type enumeration \
table.showEntryCreate false \
table.format yyyy/MM/dd \
table.addressTemplate _quote_\${first_name} \${last_name}<br>\${street}<br>\${city} \${state}<br>\${zip}_quote_ 
election_year.type enumeration \
table.canlist false   \
committee.canlist true  candidate.canlist true  filing_date.canlist true  full_name.canlist true  \
street.canlist true  city.canlist true  contribution.canlist true  \
state.type enumeration \
filing_date.type date amended_date.type date transaction_date.type date 
city.type enumeration zip.type string contribution_type.type enumeration contribution_type.addnot true \
anonymous.type enumeration \
" contributions_old.csv > boulder_campaign_contributionsdb.xml

exit
sh /Users/jeffmc/source/ramadda/bin/scpgeode.sh 50.112.99.202 contributions_final.csv staging
sh /Users/jeffmc/source/ramadda/bin/scpgeode.sh 50.112.99.202  boulder_campaign_contributionsdb.xml plugins


