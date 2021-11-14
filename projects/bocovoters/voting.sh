#!/bin/sh
mydir=`dirname $0`
source ${mydir}/init.sh



do_histogram() {
    fetch_voting_report
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
	   -p ${datadir}/ce-068-2017.txt.zip > voting_report_2017.csv


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
	   -p ${datadir}/ce-068-2017.txt.zip >voting_report_2019.csv

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
	   -p ${datadir}/ce-068-2017.txt.zip > voting_report_2020.csv

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
	   -p ${datadir}/ce-068-2017.txt.zip > voting_report_2021.csv

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
	   -join precinct latitude,longitude ${datadir}/boco_precincts.csv  precinct NaN \
	   -sort voted_date \
	   -addheader "voted_date.type date voted_date.format yyyy-MM-dd" \
	   -p voting_report_2021.csv > boulder_voting_2021_map.csv

    stage_local *histogram.csv 

}

do_histogram
