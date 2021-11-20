#!/bin/sh
mydir=`dirname $0`
source ${mydir}/init.sh
bins="18,25,35,45,55,65,75"    



<<comment
comment


year=2017

make_age_histogram () {
    year="$1"

    ${csv} -cleaninput -dots ${dots} \
	   -insert "" voted 0 \
	   -if -notpattern voted_date "" -setcol "" "" voted 1 -endif \
	   -histogram age "${bins}"  "voted" "sum" \
	   -insert 0  year $year \
	   -func "Turnout" "100*(_voted_sum)/_count" \
	   -decimals turnout 1 \
	   -p voting_report_${year}.csv > voted_${year}.csv


    ${csv} -cleaninput -dots ${dots} \
	   -notpattern voted_date "" \
	   -summary "count,avg" voted_date age "" \
	   -gt count 150 \
	   -sort voted_date \
	   -notpattern voted_date 2017-11-17 \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_${year}.csv > boulder_voting_${year}_histogram.csv

    ${csv} -cleaninput -dots ${dots} \
	   -notpattern voted_date "" \
	   -histogram age "${bins}"  "" "" \
	   -addheader "" \
	   -p voting_report_${year}.csv > boulder_voting_${year}_age_histogram.csv
}    

do_histogram() {
#    fetch_voting_report
    year=2016
    echo "doing ${year} histograms"
    ${csv} -delimiter "|"  -cleaninput -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "-16$" "-${year}" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -p ${datadir}/ce-068-${year}.txt.zip > voting_report_${year}.csv
    make_age_histogram $year


    year=2017
    echo "doing ${year} histograms"
    ${csv} -delimiter "|"  -cleaninput -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "-17$" "-${year}" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -notpattern voted_date 2017-11-17 \
	   -p ${datadir}/ce-068-${year}.txt.zip > voting_report_${year}.csv


    make_age_histogram $year

    year=2018
    echo "doing ${year} histograms"
    ${csv} -delimiter "|"  -cleaninput -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "-18$" "-2018" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -p ${datadir}/ce-068-${year}.txt.zip > voting_report_${year}.csv


    make_age_histogram $year



    year=2019
    echo "doing ${year} histogram"
    ${csv} -delimiter "|"   -cleaninput -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "-19" "-2019" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -p ${datadir}/ce-068-${year}.txt.zip >voting_report_${year}.csv

    make_age_histogram $year

    year=2020
    echo "doing ${year} histograms"
    ${csv} -delimiter "|"  -cleaninput -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -p ${datadir}/ce-068-${year}.txt.zip > voting_report_${year}.csv

    make_age_histogram $year

    year=2021
    echo "doing ${year} histograms"
    ${csv} -delimiter "|" -cleaninput -dots ${dots} \
	   -ifin voter_id voters_boulder.csv  voter_id  \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" 05-NOV-09 \
	   -notpattern "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" ".*SEP.*" \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "-19" "-2019" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -p ${datadir}/ce-068-${year}.txt.zip > voting_report_${year}.csv

    make_age_histogram $year


    ${csv}  -cleaninput -dots ${dots} \
	   -summary "count" precinct "" "voted_date" \
	   -join precinct latitude,longitude ${datadir}/boco_precincts.csv  precinct NaN \
	   -sort voted_date \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_${year}.csv > boulder_voting_${year}_map.csv

    ${csv} -deheader boulder_voting_2017_age_histogram.csv -p > tmp2017.csv
    ${csv} -deheader boulder_voting_2019_age_histogram.csv -p > tmp2019.csv
    ${csv} -deheader boulder_voting_2021_age_histogram.csv -p > tmp2021.csv
    ${csv} -join age_range  count tmp2021.csv age_range 0 \
	   -set 1 0 "2019 Votes" \
	   -set  3 0 "2021 Votes" -notcolumns percent \
	   -func "Difference" "_2021_votes-_2019_votes" \
	   -func "Percent Change" "100*(_2021_votes-_2019_votes)/_2019_votes" \
	   -round percent_change \
	   -addheader "" \
	   -p tmp2019.csv > boulder_voting_2019_2021.csv

    ${csv} -join age_range  count tmp2021.csv age_range 0 \
	   -set 1 0 "2017 Votes" \
	   -set  3 0 "2021 Votes" -notcolumns percent \
	   -func "Difference" "_2021_votes-_2017_votes" \
	   -func "Percent Change" "100*(_2021_votes-_2017_votes)/_2017_votes" \
	   -round percent_change \
	   -addheader "" \
	   -p tmp2017.csv > boulder_voting_2017_2021.csv
    

    echo "Making boulder voters by age"
    ${csv} -cleaninput -dots ${dots} \
	   -pattern status Active \
	   -columns yob \
	   -func age "2021-_yob" \
	   -histogram age "${bins}"  "" "" \
	   -set 1 0 "Total voters" \
	   -join age_range count tmp2021.csv  age_range 0 \
	   -set 3 0 "2021 Votes" \
	   -join age_range count tmp2019.csv  age_range 0 \
	   -set 4 0 "2019 Votes" \
	   -join age_range count tmp2017.csv  age_range 0 \
	   -set 5 0 "2017 Votes" \
	   -func "2017 Turnout" "100*(_2017_votes)/_total_voters" \
	   -decimals 2017_turnout 1 \
	   -func "2019 Turnout" "100*(_2019_votes)/_total_voters" \
	   -decimals 2019_turnout 1 \
	   -func "2021 Turnout" "100*(_2021_votes)/_total_voters" \
	   -decimals 2021_turnout 1 \
	   -addheader "2017_turnout.unit %  2019_turnout.unit %  2021_turnout.unit % " \
	   -p ${datadir}/voters_boulder.csv.zip  > boulder_voters_age.csv
    stage_local boulder_voters_age.csv
    stage_local boulder_voting_2019_2021.csv
    stage_local *histogram.csv
    stage_local boulder_voting_2017_2021.csv

}

do_turnout() {
    ${csv} -append  1 voted_2016.csv voted_2017.csv voted_2018.csv voted_2019.csv voted_2020.csv voted_2021.csv > tmp.csv
    ${csv} -makefields age_range turnout year "" \
	   -addheader "18_25.unit % 35_45.unit % 45_55.unit % 55_65.unit % 75.unit % year.type string year.format yyyy" -p tmp.csv >boulder_voting_turnout.csv

    stage_local boulder_voting_turnout.csv
}    


do_histogram
do_turnout
