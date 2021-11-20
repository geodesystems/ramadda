#!/bin/sh
mydir=`dirname $0`
source ${mydir}/init.sh


do_fetch() {
    wget -O ${datadir}/2021_contributions.csv --post-data="exportType=Contribution&electionID=20&committeeID=-1&filingDateStart=&filingDateStop=&transactionDateStart=&transactionDateStop=" https://election.bouldercolorado.gov/electionContributions.php
}

do_convert() {
    echo "converting old"
    ${csv} -dots 100 -set Match 0 MatchAmount -set CommitteeNum 0 CommitteeNumber  \
	   -case city proper \
	   -change city "^(Bouder|Booulder|Boudler|Bouilder|Boukder|Boul;der|Boulde|Boulder County|Boulder-Co|Bouldera|Bouldewr|Bouldewwr|Bouldr|Bouldwer|Bouler|Bouolder|Bouulder)$" Boulder \
	   -change filingdate,amendeddate,transactiondate "(....)/(..)/(..).*" "\$1/\$2/\$3" \
	   -change filingdate,amendeddate,transactiondate "(....)-(..)-(..).*" "\$1/\$2/\$3" \
	   -change filingdate,amendeddate,transactiondate "Invalid Date" "" \
    	   -case  FromCandidate lower \
	   -case  anonymous lower \
	   -p ${datadir}/Election_Contributions.csv > oldtmp.csv
    echo "converting new"
    ${csv} -dots 100 -notcolumns "YTDAmount,AmendsContributionID,ContributionID" \
	   -case city proper \
	   -change city "^(Bolder|Bouder|Booulder|Boudler|Bouilder|Boukder|Boul;der|Boulde|Boulderr||Boulder County|Boulder-Co|Bouldera|Bouldewr|Bouldewwr|Bouldr|Bouldwer|Bouler|Bouolder|Bouulder)$" Boulder \
	   -change filingdate,amendeddate,transactiondate "(..)/(..)/(....)" "\$3/\$1/\$2" \
	   -change filingdate,amendeddate,transactiondate "(....)-(..)-(..).*" "\$1/\$2/\$3" \
	   -change filingdate,amendeddate,transactiondate "Invalid Date" "" \
	   -case  FromCandidate lower \
	   -case  anonymous lower \
	   -geocodeaddressdb street,city,state "" "" \
	   -p  ${datadir}/2021_contributions.csv > newtmp.csv
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
	   -change full_name "file:${mydir}/contributions_patterns.txt" "" \
	   -case full_name,first_name,last_name proper \
	   -columnsbefore last_name full_name \
	   -set amendeddate	 0 amended_date	   \
	   -change contribution,fromcandidate,matchamount "_dollar_" "" \
	   -change contribution,fromcandidate,matchamount "_leftparen_(.*)_rightparen_" "-\$1" \
	   -change contribution,fromcandidate,matchamount "," "" \
	   -change Contribution "_dollar_" "" \
	   -p ${infile} > ${outfile}
}


#do_fetch
echo "converting"
do_convert
echo "making old"
do_contributions oldtmp.csv    contributions_old.csv
echo "making new"
do_contributions newtmp.csv    contributions_new.csv

cp contributions_old.csv contributions_final.csv
tail -n+2 contributions_new.csv >>contributions_final.csv
echo "making db"
${csv} -db " table.id boulder_campaign_contributions table.label {Boulder Campaign Contributions} \
table.cansearch false table.canlist false \
table.include file:${mydir}/contributions_include.txt \
committee.cansearch true  committee.canlist true   \
type.cansearch true  type.canlist true   \
candidate.cansearch true  candidate.canlist true   \
filing_date.cansearch true  filing_date.canlist true   \
official_filing.cansearch true   \
election_year.cansearch true  election_year.canlist true   \
full_name.islabel true committee.islabel true \
full_name.cansearch true  full_name.canlist true   \
contribution.cansearch true  contribution.canlist true   \
contribution.dostats true \
contribution_type.cansearch true  contribution_type.canlist true   \
from_candidate.cansearch true    \
committee.numberOfSearchWidgets 4 \
committee.type enumeration type.type enumeration candidate.type enumeration \
candidate.numberOfSearchWidgets 4 \
official_filing.type enumeration \
from_candidate.type enumeration \
location.type latlon \
table.showEntryCreate false \
table.format yyyy/MM/dd \
table.addressTemplate _quote_\${first_name} \${last_name}<br>\${street}<br>\${city} \${state}<br>\${zip}_quote_ 
election_year.type enumeration \
table.canlist false   \
committee.canlist true  candidate.canlist true  filing_date.canlist true  full_name.canlist true  \
street.canlist true  city.type  enumeration \
city.canlist true  city.cansearch true city.addnot true \
contribution.canlist true  \
state.type enumeration state.cansearch true state.addnot true \
filing_date.type date amended_date.type date transaction_date.type date 
zip.type string contribution_type.type enumeration acontribution_type.addnot true \
anonymous.type enumeration \
" contributions_new.csv > boulder_campaign_contributionsdb.xml


release_plugin boulder_campaign_contributionsdb.xml 
stage_local  contributions_new.csv  contributions_final.csv 
stage_ramadda contributions_new.csv  contributions_final.csv 



