#!/bin/sh
mydir=`dirname $0`
set -e
export csv=~/bin/csv.sh 

dots=5000
#registered_voters=source/ce-vr011b.txt
registered_voters=source/ce-vr011d.txt
voting_report=source/ce-068-2021.txt
source=voters_boulder.csv
unique_voter_history=voter_history_unique.csv
precincts=source/boco_precincts.csv
geocodio=source/voters_addresses_geocodio.csv

voting_report_url=https://election.boco.solutions/ElectionDataPublicFiles/CE-068_Voters_With_Ballots_List_Public.zip
ex=bC!Erlction!$


do_all() {
    init_files
    do_demographics
    do_prep
    do_history
    do_counts
    do_joins
    do_histogram
    do_final
    do_db
    do_release
}



init_files() {
    echo "fetching voter report"
    wget  -O CE-068_Voters_With_Ballots_List_Public.zip ${voting_report_url}
    cd tmp
    jar -xvf ../CE-068_Voters_With_Ballots_List_Public.zip
    cp ../source/ce-068-2021.txt ../source/bak/ce-068-2021-last.txt 
    mv *.txt ../source/ce-068-2021.txt
    cd ..
    echo "initializing files"
    cp source/Master_Voting_History_List_Part1.csv voter_history.csv
    tail -n+2 source/Master_Voting_History_List_Part2.csv >>voter_history.csv
    tail -n+2 source/Master_Voting_History_List_Part3.csv >>voter_history.csv        
}

#init_files
#exit

do_prep() {
    echo "processing voting report"
    ${csv}  -delimiter "|" 	 -dots ${dots}    -pattern RES_CITY BOULDER \
	    -columns voter_id,MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE \
	    -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "" voted_in_2021 \
	    -trim voted_in_2021 \
	    -change voted_in_2021 "^$" false \
	    -change voted_in_2021 ".*[0-9]+.*" true \
	    -p ${voting_report}  > voted_in_2021.csv
    ${csv}  -delimiter "|" 	 -dots ${dots}   \
	    -columns voter_id,MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE \
	    -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "" voted_in_2021 \
	    -trim voted_in_2021 \
	    -change voted_in_2021 "^$" false \
	    -change voted_in_2021 ".*[0-9]+.*" true \
	    -p ${voting_report}  > all_voted_in_2021.csv


    echo "processing registered voters"

#    ${csv}  -delimiter "|"  -dots ${dots}  -notcolumns "regex:(?i)BALLOT_.*"  -pattern res_city BOULDER  -p ${registered_voters} > voters_base.csv

    ${csv}  -delimiter "|"  -dots ${dots}  -notcolumns "regex:(?i)BALLOT_.*"  -concat precinct,split "." precinct_split  \
	    -ifin  split source/boulder_splits.csv precinct_split -notcolumns precinct_split -p ${registered_voters} > voters_base.csv        

    ${csv} -if -pattern mail_addr1,mailing_country "^$" -copycolumns res_address mail_addr1  -endif\
	   -if -pattern mailing_city,mailing_country "^$" -copycolumns res_city mailing_city  -endif \
	   -if -pattern mailing_state,mailing_country "^$" -copycolumns res_state mailing_state  -endif\
	   -if -pattern mailing_zip,mailing_country "^$" -copycolumns res_zip_code mailing_zip  -endif\
	   -p voters_base.csv > ${source}
    ${csv} -columns res_address,res_city -change res_address " APT .*" "" -change res_address " UNIT .*" "" -trim res_address -unique res_address -insert "" state Colorado  -set 0 0 address -set 1 0 city -dots ${dots} -p ${source} > voters_addresses.csv
    ${csv} -sample 0.01  -p voters_addresses.csv > voters_addresses_short.csv        
#    rm voters_base.csv
}

#do_prep
#exit

do_histogram() {
    bins="18,25,35,45,50,75"
    bins="18,25,35,45,55,65,75"    
    echo "doing 2017 histograms"
    ${csv} -delimiter "|" \
	   -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -notpattern voted_date "" \
	   -change voted_date "-17$" "-2017" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "2017-_yob" \
	   -notpattern voted_date 2017-11-17 \
	   -p source/ce-068-2017.txt > voting_report_2017.csv
    
    ${csv} -dots ${dots} \
	   -summary "count,avg" voted_date age "" \
	   -gt count 20 \
	   -sort voted_date \
	   -notpattern voted_date 2017-11-17 \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_2017.csv > boulder_voting_2021_histogram.csv

    ${csv} -dots ${dots} \
	   -histogram age "${bins}" \
	   -addheader "" \
	   -p voting_report_2017.csv > boulder_voting_2017_age_histogram.csv

    echo "doing 2019 histogram"
    ${csv} -delimiter "|" \
	   -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -notpattern voted_date "" \
	   -change voted_date "-19" "-2019" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "2019-_yob" \
	   -p source/ce-068-2019.txt >voting_report_2019.csv

    ${csv} -dots ${dots} \
	   -summary "count,avg" voted_date age "" \
	   -decimals age_avg 1\
	   -gt count 20 \
	   -sort voted_date \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_2019.csv > boulder_voting_2019_histogram.csv

    ${csv} -dots ${dots} \
	   -histogram age "${bins}" \
	   -addheader "" \
	   -p voting_report_2019.csv > boulder_voting_2019_age_histogram.csv



    echo "doing 2020 histograms"
    ${csv} -delimiter "|" \
	   -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -notpattern voted_date "" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "2020-_yob" \
	   -p source/ce-068-2020.txt > voting_report_2020.csv

    ${csv} -dots ${dots} \
	   -summary "count,avg" voted_date age "" \
	   -gt count 20 \
	   -sort voted_date \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_2020.csv > boulder_voting_2020_histogram.csv

    ${csv} -dots ${dots} \
	   -histogram age "${bins}" \
	   -addheader "" \
	   -p voting_report_2020.csv > boulder_voting_2020_age_histogram.csv


    echo "doing 2021 histograms"
    ${csv} -delimiter "|" \
	   -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -notpattern voted_date "" \
	   -change voted_date "-19" "-2019" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "2021-_yob" \
	   -p source/ce-068-2021.txt > voting_report_2021.csv

    ${csv} -dots ${dots} \
	   -summary "count,avg" voted_date age "" \
	   -decimals age_avg 1\
	   -gt count 20 \
	   -sort voted_date \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_2021.csv > boulder_voting_2021_histogram.csv

    ${csv} -dots ${dots} \
	   -histogram age "${bins}" \
	   -addheader "" \
	   -p voting_report_2021.csv > boulder_voting_2021_age_histogram.csv

    ${csv}  -dots ${dots} \
	   -summary "count" precinct "" "voted_date" \
	   -join precinct latitude,longitude source/boco_precincts.csv  precinct NaN \
	   -sort voted_date \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_2021.csv > boulder_voting_2021_map.csv

    cp *histogram.csv ~/

}

do_histogram
exit
    

do_precincts() {
    ${csv} -join precinct_name active_voters source/precincts_voters.csv precinct 0   \
	   -join precinct city source/precincts_city.csv precinct ""   \
	   -columnsafter  neighborhood city \
	   -columnsafter  city active_voters \
	   -concat "latitude,longitude" ";" Location -notcolumns latitude,longitude \
	   -p ${precincts}> precincts_final.csv
    ${csv} -db "precinct.type string neighborhood.type enumeration city.type enumeration location.type latlon \
    table.icon /icons/map/marker-blue.png \
    table.defaultView map \
    table.mapLabelTemplate _quote_\${precinct} - \${neighborhood}_quote_ \
    table.id precincts table.label {Precincts}  \
    table.cansearch false \
    polygon.canlist false location.canlist false \
    polygon.type clob     polygon.size 200000 \
    precinct.cansearch true active_voters.cansearch true  neighborhood.cansearch true  city.cansearch true  location.cansearch true \
" precincts_final.csv > precinctsdb.xml
}

#do_precincts
#exit



do_history() {
   echo "making unique voting history"
   ${csv} -ifin voter_id voters_boulder.csv  voter_id -p voter_history.csv  > voter_history_boulder.csv
   ${csv} -dots ${dots} -unique  "voter_id,election_date" -p voter_history_boulder.csv > ${unique_voter_history}
   echo "making off year"
   ${csv} -dots ${dots} -pattern election_date "(11/../2001|11/../2003|11/../2005|11/../2007|11/../2009|11/../2011|11/../2013|11/../2015|11/../2017|11/../2019)" -p ${unique_voter_history} > history_offyears10.csv
   ${csv} -dots ${dots} -pattern election_date "(11/../2020)" -p ${unique_voter_history} > history_2020.csv
   ${csv} -dots ${dots} -pattern election_date "(11/../2019)" -p ${unique_voter_history} > history_2019.csv      
   ${csv} -dots ${dots} -pattern election_date "(11/../2015|11/../2017|11/../2019)" -p ${unique_voter_history} > history_offyears3.csv   
}


do_counts() {
    echo "making off year count"
    cols=VOTER_ID,count
    ${csv}   -countunique voter_id -columns ${cols} -set 1 0 "Voted in 2020" -p history_2020.csv   >tmp.csv
    ${csv}   -change voted_in_2020 1 true  -p tmp.csv   >count_2020.csv
    ${csv}   -countunique voter_id -columns ${cols} -set 1 0 "Voted in 2019" -p history_2019.csv   >tmp.csv
    ${csv}   -change voted_in_2019 1 true  -p tmp.csv   >count_2019.csv    
    ${csv}   -countunique voter_id -columns ${cols} -set 1 0 "Last 3 offyear elections" -p history_offyears3.csv   >count_offyears3.csv
    ${csv}   -countunique voter_id -columns ${cols} -set 1 0 "Last 10 offyear elections" -p history_offyears10.csv   >count_offyears10.csv
    echo "making all count"
    ${csv}   -countunique voter_id -columns ${cols} -set 1 0 "All elections" -p ${unique_voter_history}  >count_all.csv
#    echo "making municipal count"
#    ${csv} -pattern election_type Municipal  -countunique voter_id -columns ${cols} -set 1 0 "Municipal elections"  -p ${unique_voter_history}  >count_municipal.csv
    echo "making primary count"
    ${csv} -pattern election_type Primary  -countunique voter_id -columns ${cols} -set 1 0 "Primary elections"  -p ${unique_voter_history} >count_primary.csv
}

#do_counts
#exit


do_demographics() {
    echo "cleaning up the demographics"
#	-rand latitude 39.983 40.042  -rand longitude -105.303 -105.216 \
#	-notcolumns latitude,longitude \
    ${csv}  \
	-notcolumns "regex:(?i).*veteran.*" \
	-between latitude 39.955 40.1 \
	-between longitude -105.3777 -105.155 \
	-concat "latitude,longitude" ";" Location -notcolumns latitude,longitude \
    -columns "address,location,\
ACS Demographics/Median age/Total/Value, \
ACS Economics/Number of households/Total/Value, \
ACS Economics/Median household income/Total/Value, \
regex:(?i).*Percentage \
"    \
-operator "\
ACS Demographics/Population by age range/Male: Under 5 years/Percentage, \
ACS Demographics/Population by age range/Male: 5 to 9 years/Percentage, \
ACS Demographics/Population by age range/Male: 10 to 14 years/Percentage,\
ACS Demographics/Population by age range/Male: 15 to 17 years/Percentage" "Male under 18" "+" \
-operator "\
ACS Demographics/Population by age range/Male: 18 and 19 years/Percentage, \
ACS Demographics/Population by age range/Male: 20 years/Percentage, \
ACS Demographics/Population by age range/Male: 21 years/Percentage, \
ACS Demographics/Population by age range/Male: 22 to 24 years/Percentage, \
ACS Demographics/Population by age range/Male: 25 to 29 years/Percentage" "Male 18 to 30" "+" \
-operator "\
ACS Demographics/Population by age range/Male: 30 to 34 years/Percentage, \
ACS Demographics/Population by age range/Male: 35 to 39 years/Percentage, \
ACS Demographics/Population by age range/Male: 40 to 44 years/Percentage, \
ACS Demographics/Population by age range/Male: 45 to 49 years/Percentage, \
ACS Demographics/Population by age range/Male: 50 to 54 years/Percentage, \
ACS Demographics/Population by age range/Male: 55 to 59 years/Percentage" "Male 30 to 60" "+" \
-operator "\
ACS Demographics/Population by age range/Male: 60 and 61 years/Percentage, \
ACS Demographics/Population by age range/Male: 62 to 64 years/Percentage, \
ACS Demographics/Population by age range/Male: 65 and 66 years/Percentage, \
ACS Demographics/Population by age range/Male: 67 to 69 years/Percentage, \
ACS Demographics/Population by age range/Male: 70 to 74 years/Percentage, \
ACS Demographics/Population by age range/Male: 75 to 79 years/Percentage,\
ACS Demographics/Population by age range/Male: 80 to 84 years/Percentage, \
ACS Demographics/Population by age range/Male: 85 years and over/Percentage" "Male 60 plus" "+" \
-operator "\
ACS Demographics/Population by age range/Female: Under 5 years/Percentage, \
ACS Demographics/Population by age range/Female: 5 to 9 years/Percentage, \
ACS Demographics/Population by age range/Female: 10 to 14 years/Percentage,\
ACS Demographics/Population by age range/Female: 15 to 17 years/Percentage" "Female under 18" "+" \
-operator "\
ACS Demographics/Population by age range/Female: 18 and 19 years/Percentage, \
ACS Demographics/Population by age range/Female: 20 years/Percentage, \
ACS Demographics/Population by age range/Female: 21 years/Percentage, \
ACS Demographics/Population by age range/Female: 22 to 24 years/Percentage, \
ACS Demographics/Population by age range/Female: 25 to 29 years/Percentage" "Female 18 to 30" "+" \
-operator "\
ACS Demographics/Population by age range/Female: 30 to 34 years/Percentage, \
ACS Demographics/Population by age range/Female: 35 to 39 years/Percentage, \
ACS Demographics/Population by age range/Female: 40 to 44 years/Percentage, \
ACS Demographics/Population by age range/Female: 45 to 49 years/Percentage, \
ACS Demographics/Population by age range/Female: 50 to 54 years/Percentage, \
ACS Demographics/Population by age range/Female: 55 to 59 years/Percentage" "Female 30 to 60" "+" \
-operator "\
ACS Demographics/Population by age range/Female: 60 and 61 years/Percentage, \
ACS Demographics/Population by age range/Female: 62 to 64 years/Percentage, \
ACS Demographics/Population by age range/Female: 65 and 66 years/Percentage, \
ACS Demographics/Population by age range/Female: 67 to 69 years/Percentage, \
ACS Demographics/Population by age range/Female: 70 to 74 years/Percentage, \
ACS Demographics/Population by age range/Female: 75 to 79 years/Percentage,\
ACS Demographics/Population by age range/Female: 80 to 84 years/Percentage, \
ACS Demographics/Population by age range/Female: 85 years and over/Percentage" "Female 60 plus" "+" \
-operator "Male under 18,Female under 18" "Age under 18" average \
-notcolumns "Male under 18,Female under 18" \
-operator "Male 18 to 30,Female 18 to 30" "Age 18 to 30" average \
-notcolumns "Male 18 to 30,Female 18 to 30"  \
-operator "Male 30 to 60,Female 30 to 60" "Age 30 to 60" average \
-notcolumns "Male 30 to 60,Female 30 to 60" \
-operator "Male 60 plus,Female 60 plus" "Age 60 plus" average \
-notcolumns "Male 60 plus,Female 60 plus"  \
-notcolumns "regex:(?i).*/Female.*" -notcolumns "regex:(?i).*/Male.*"  \
-operator "ACS Economics/Household income/Less than \$10_000/Percentage" "Income less than 10000" "+" \
-operator "\
ACS Economics/Household income/\$10_000 to \$14_999/Percentage, \
ACS Economics/Household income/\$15_000 to \$19_999/Percentage, \
ACS Economics/Household income/\$20_000 to \$24_999/Percentage, \
ACS Economics/Household income/\$25_000 to \$29_999/Percentage" "Income 10000 to 30000" + \
-operator "\
ACS Economics/Household income/\$30_000 to \$34_999/Percentage, \
ACS Economics/Household income/\$35_000 to \$39_999/Percentage, \
ACS Economics/Household income/\$40_000 to \$44_999/Percentage, \
ACS Economics/Household income/\$45_000 to \$49_999/Percentage, \
ACS Economics/Household income/\$50_000 to \$59_999/Percentage,\
ACS Economics/Household income/\$60_000 to \$74_999/Percentage,  \
ACS Economics/Household income/\$75_000 to \$99_999/Percentage" "Income  30000 to 100000" + \
-operator "\
ACS Economics/Household income/\$100_000 to \$124_999/Percentage, \
ACS Economics/Household income/\$125_000 to \$149_999/Percentage, \
ACS Economics/Household income/\$150_000 to \$199_999/Percentage, \
ACS Economics/Household income/\$200_000 or more/Percentage" "Income 100000 plus" + \
-operator "ACS Demographics/Race and ethnicity/Hispanic or Latino/Percentage" "Percent Hispanic" "+" \
-operator "ACS Demographics/Median age/Total/Value" "Median age" "+" \
-operator "ACS Families/Household type by household/Family households/Percentage" "Percent family household" "+" \
-operator "ACS Families/Household type by household/Nonfamily households/Percentage" "Percent non family household" "+" \
-operator "ACS Housing/Ownership of occupied units/Owner occupied/Percentage" "Percent owner occupied" "+" \
-operator "ACS Housing/Ownership of occupied units/Renter occupied/Percentage" "Percent renter occupied" "+" \
-operator " \
ACS Housing/Value of owner_occupied housing units/Less than \$10_000/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$10_000 to \$14_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$15_000 to \$19_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$20_000 to \$24_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$25_000 to \$29_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$30_000 to \$34_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$35_000 to \$39_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$40_000 to \$49_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$50_000 to \$59_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$60_000 to \$69_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$70_000 to \$79_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$80_000 to \$89_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$90_000 to \$99_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$100_000 to \$124_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$125_000 to \$149_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$150_000 to \$174_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$175_000 to \$199_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$200_000 to \$249_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$250_000 to \$299_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$300_000 to \$399_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$400_000 to \$499_999/Percentage" "House value to 500000" "+" \
-operator " \
ACS Housing/Value of owner_occupied housing units/\$500_000 to \$749_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$750_000 to \$999_999/Percentage" "House value 500000 to 1 million" "+" \
-operator " \
ACS Housing/Value of owner_occupied housing units/\$1_000_000 to \$1_499_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$1_500_000 to \$1_999_999/Percentage, \
ACS Housing/Value of owner_occupied housing units/\$2_000_000 or more/Percentage"  "House value greater 1 million" "+" \
-scale "age_under_18,age_18_to_30,age_30_to_60,age_60_plus,income_less_than_10000,income_10000_to_30000,income_30000_to_100000,income_100000_plus,percent_hispanic,percent_family_household,percent_non_family_household,percent_owner_occupied,percent_renter_occupied,house_value_to_500000,house_value_500000_to_1_million,house_value_greater_1_million" 0 100 0 \
-round "age_under_18,age_18_to_30,age_30_to_60,age_60_plus,income_less_than_10000,income_10000_to_30000,income_30000_to_100000,income_100000_plus,percent_hispanic,percent_family_household,percent_non_family_household,percent_owner_occupied,percent_renter_occupied,house_value_to_500000,house_value_500000_to_1_million,house_value_greater_1_million" \
-notcolumns "regex:(?i).*Housing/.*" \
-notcolumns "regex:(?i).* structure/.*" \
-notcolumns "ACS Demographics/Median age/Total/Value" \
-notcolumns "regex:(?i).*/household.*" \
-notcolumns "regex:(?i).*household/.*" \
-notcolumns "regex:(?i).*households/.*" \
-notcolumns "regex:(?i).*ethnicity/.*" \
-notcolumns "regex:(?i).*income/.*" \
-p  ${geocodio} >  voters_geocode_trim.csv
}


#do_demographics
#exit


do_joins() {
    echo "doing joins"
    infile=${source}
    cp ${infile} working.csv
    ${csv} -join precinct_name precinct_turnout_2019 source/precincts_turnout.csv precinct 0 -p working.csv > tmp.csv
    mv tmp.csv working.csv
    ${csv} -join 0 voted_in_2021 voted_in_2021.csv voter_id false   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv
    ${csv} -join 0 1 count_2020.csv voter_id false   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv
    ${csv} -join 0 1 count_2019.csv voter_id false   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv    
    ${csv} -join 0 1 count_offyears3.csv voter_id 0   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv
    ${csv} -join 0 1 count_offyears10.csv voter_id 0   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv
    ${csv} -join 0 1  count_all.csv voter_id 0   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv
    ${csv} -join 0 1 count_primary.csv voter_id 0   -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv    
#    ${csv} -join 0 1 count_municipal.csv voter_id 0   -dots ${dots} -p working.csv > tmp.csv
#    mv tmp.csv working.csv
#join the precincts
    ${csv} -join 0 1 ${precincts} precinct  "" -dots ${dots} -p working.csv > tmp.csv
    mv tmp.csv working.csv
#join the  demographics

#create a new column and remove the UNIT and APT suffix to do the join with the geocoded addresses
    ${csv} -copy res_address res_address_trim -change res_address_trim " APT .*" "" -change res_address_trim " UNIT .*" "" -p working.csv > tmp.csv
    mv tmp.csv working.csv

    do_join_demographics working.csv tmp.csv
    mv tmp.csv working.csv

##Delete the temp address
    ${csv} -notcolumns res_address_trim -p working.csv > tmp.csv


    mv tmp.csv voters_joined.csv
    rm working.csv
}



do_join_demographics() {
    echo "doing demographics join"
    ${csv} -join address "*" voters_geocode_trim.csv res_address_trim "0"  -dots ${dots} -p $1 > $2
}

#do_joins


do_final() {
    echo "making final"
##	    -ifin precinct_name source/city_boulder_precincts.csv precinct \
    ${csv}  \
	    -notcolumns MUNICIPALITY,city_ward,county,preference,uocava,uocava_type,issue_method,split \
	    -set county_regn_date 0 registration_date  \
	    -set vr_phone 0 phone -set voter_name 0 name  -set yob 0 birth_year \
	    -ranges birth_year "Birth year range" 1930 10 \
	    -columnsafter birth_year birth_year_range \
	    -set res_address 0 address -set res_city 0 city -set res_state 0 state \
	    -set res_zip_code 0 zip_code -set res_zip_plus 0 zip_plus \
	    -columnsbefore first_name  name,party,status,status_reason,gender,address,city,state,zip_code \
	    -columnsbefore city  street_name,neighborhood \
	    -columnsafter  city  location  \
	    -concat street_name,street_type " " "full street name" \
	    -columnsafter street_name full_street_name \
	    -columnsafter precinct precinct_turnout_2019 \
	    -even address \
	    -set even 0 "Address even" \
	    -notpattern address "1731 HAWTHORN AVE" \
	    -p  voters_joined.csv > voters_final.csv
}

#do_final
#exit


do_db() {
    echo "making db"
    ${csv} -db "table.id boco_voters table.name {Boulder County Voters}  \
    table.icon /db/user.png \
    table.showEntryCreate false \
    table.format  MM/dd/yyyy table.defaultOrder {full_street_name,asc;address_even;address,asc} \
    table.showDateView false table.showChartView false table.showFeedView false \
    table.formjs file:${mydir}/formjs.js \
table.cansearch false table.searchForLabel {Basic Voter Properties} \
table.mapLabelTemplate _quote_\${name}_quote_ \
table.mapLabelTemplatePrint _quote_<div style='display: flex;  justify-content: space-between;margin-right:5px;'><span>\${name}</span><span>\${address}</span></div>_quote_ \
table.addressTemplate _quote_\${name}<br>\${address}<br>\${city} \${state}<br>\${zip_code}_quote_ table.canlist false   \
voter_id.type string \
address_even.cansort true status.canlist true city.canlist true mailing_country.help {Enter &quot;_blank_&quot; to search for empty country} mailing_country.cansearch true mailing_country.addnot true \
name.canlist true birth_year.canlist true gender.canlist true  \
party.canlist true  status.canlist true  \
address.addnot true address.addfiletosearch true address.canlist true  phone.canlist true \
mail_addr1.cansearch true mail_addr1.addnot true mail_addr1.addfiletosearch true \
registration_date.cansearch true  neighborhood.cansearch true  city.cansearch true name.cansearch true  \
birth_year_range.cansearch true birth_year_range.type enumeration \
birth_year.cansearch true  yob.cansearch true gender.cansearch true party.cansearch true status.cansearch true status_reason.cansearch true precinct.addnot true precinct.cansearch true  precinct.addfiletosearch true \
precinct_turnout_2019.cansearch true address.cansearch true \
party.values {REP:Republican,UAF:Unaffiliated,DEM:Democrat,GRN:Green,LBR:Labor,ACN:American Constitution Party,UNI:Unity,APV:Approval Voting} \
precinct_turnout_2019.placeholder {0-100} \
voted_in_2021.type enumeration voted_in_2021.cansearch true  voted_in_2021.group {Voting History} voted_in_2021.suffix {Not working until 3 weeks before the election} \
voted_in_2020.type enumeration voted_in_2020.cansearch true  \
voted_in_2019.type enumeration voted_in_2019.cansearch true  \
.*elections.cansearch true  .*elections.type int \
last_3_offyear_elections.suffix {# Times Voted in last 3 off year elections}\
last_10_offyear_elections.suffix {# Times Voted in last 10 off year elections}\
 phone.type string house_number.type string unit_number.type string zip_code.type string zip_plus.type string  \
full_street_name.cansearch true \
eff_date.type date polparty_aff_date.type date registration_date.type date \
status.type enumeration neighborhood.type enumeration neighborhood.numberOfSearchWidgets 3 status_reason.type enumeration \
precinct.placeholder {One or more precinct ids} \
precinct.lookupdb precincts:precinct precinct.type list party.type enumeration party.numberOfSearchWidgets 3 \
gender.type enumeration preference.type enumeration city.type enumeration city.numberOfSearchWidgets 3 mailing_city.type enumeration zip_code.type enumeration  mailing_zip.type enumeration  birth_year.type int \
location.type latlon location.cansearch true  \
age_under_18.placeholder {0-100} age_18_to_30.placeholder {0-100} age_30_to_60.placeholder {0-100} age_60_plus.placeholder {0-100} income_less_than_10000.placeholder {0-100} income_10000_to_30000.placeholder {0-100} income_30000_to_100000.placeholder {0-100} income_100000_plus.placeholder {0-100} percent_hispanic.placeholder {0-100} median_age.placeholder {0-100} percent_family_household.placeholder {0-100} percent_non_family_household.placeholder {0-100} percent_owner_occupied.placeholder {0-100} percent_renter_occupied.placeholder {0-100} house_value_to_500000.placeholder {0-100} house_value_500000_to_1_million.placeholder {0-100} house_value_greater_1_million.placeholder {0-100} \
age_under_18.group {Demographics} age_under_18.help {Specify ranges for percent households: 0-100} \
age_under_18.cansearch true  age_18_to_30.cansearch true  age_30_to_60.cansearch true   \
age_60_plus.cansearch true  income_less_than_10000.cansearch true  income_10000_to_30000.cansearch true  income_30000_to_100000.cansearch true  \
income_100000_plus.cansearch true  percent_hispanic.cansearch true  median_age.cansearch true  percent_family_household.cansearch true  \
percent_non_family_household.cansearch true  percent_owner_occupied.cansearch true  percent_renter_occupied.cansearch true  percent_house_value_to_500000.cansearch true  \
percent_house_value_500000_to_1_million.cansearch true  percent_house_value_greater_1_million.cansearch true"  \
voters_final.csv > bocovotersdb.xml
 }


do_release() {
    echo "Copying to geode"
    cp bocovotersdb.xml  ~/.ramadda/plugins
    sh /Users/jeffmc/source/ramadda/bin/scpgeode.sh 50.112.99.202 voters_final.csv staging
    sh /Users/jeffmc/source/ramadda/bin/scpgeode.sh 50.112.99.202 bocovotersdb.xml plugins
}


#do_db
#do_release
#exit

do_all
