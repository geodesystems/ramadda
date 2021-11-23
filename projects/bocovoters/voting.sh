#!/bin/sh
mydir=`dirname $0`
source ${mydir}/init.sh
bins="18,25,35,45,55,65,75"    
#bins="18,22,30,35,40,45,50,55,60,65,70,75"


make_histogram() {
    year="$1"
    yy="$2"
    echo "making $year histogram"

    ${csv} -cleaninput -dots ${dots} -delimiter "|"  \
	   -c "precinct,split,yob,MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" \
	   -concat precinct,split "." full_precinct \
	   -ifin split ${datadir}/boulder_splits.csv  full_precinct \
	   -concat "MAIL_BALLOT_RECEIVE_DATE,IN_PERSON_VOTE_DATE" "," "voted_date" \
	   -change voted_date "," "" \
	   -change voted_date "-${yy}$" "-${year}" \
	   -change voted_date "OCT" "10" \
	   -change voted_date "NOV" "11" \
	   -change voted_date "(..)-(..)-(....)" "\$3-\$2-\$1" \
	   -change voted_date "(..)/(..)/(....)" "\$3-\$1-\$2" \
	   -func age "${year}-_yob" \
	   -insert "" voted 0 \
	   -if -notpattern voted_date "" -setcol "" "" voted 1 -endif \
	   -p  ${datadir}/ce-068-${year}.txt.zip > ballots_sent_${year}.csv


    ${csv} -cleaninput -histogram age "${bins}"  "voted" "count,sum" \
	   -insert 0  year $year \
	   -round voted_sum  \
	   -set voted_count 0 "Registered Voters" \
	   -set voted_sum 0 "Number Voted" \
	   -func "Turnout" "100*(_number_voted/_registered_voters)" \
	   -decimals "turnout" 1 \
	   -p ballots_sent_${year}.csv > histogram_${year}.csv

    ${csv} -addheader "" -p histogram_${year}.csv > voting_age_ranges_${year}.csv

    stage_local voting_age_ranges_${year}.csv
}


do_turnout() {
    ${csv} -append  1 histogram_2016.csv histogram_2017.csv histogram_2018.csv histogram_2019.csv histogram_2020.csv histogram_2021.csv > tmp.csv

    ${csv} -makefields age_range turnout year "" \
	   -addheader "default.unit %  year.type string year.unit {} year.format yyyy" -p tmp.csv >boulder_age_turnout.csv
    ${csv} -makefields age_range registered_voters year "" \
	   -addheader "year.type string  year.format yyyy" -p tmp.csv >boulder_age_registered_voters.csv    
    ${csv} -makefields age_range number_voted year "" \
	   -addheader "year.type string year.format yyyy" -p tmp.csv >boulder_age_voted.csv
    stage_local boulder_age_*.csv

}    


do_diff() {
    year1=$1
    year2=$2
    to=boulder_voting_${year1}_${year2}.csv
    echo "Making difference between ${year1} and ${year2}"
    ${csv} -columns age_range,number_voted \
	   -join age_range  number_voted histogram_${year2}.csv age_range 0 \
	   -set 1 0 "${year1} Votes" \
	   -set  2 0 "${year2} Votes" \
	   -func "Difference" "_${year2}_votes-_${year1}_votes" \
	   -func "Percent Change" "100*(_${year2}_votes-_${year1}_votes)/_${year1}_votes" \
	   -round percent_change \
	   -addheader "percent_change.unit %" \
	   -p histogram_${year1}.csv > $to
    stage_local $to
}

do_diff 2019 2021
do_diff 2017 2021
exit
make_histogram 2016 16
make_histogram 2017 17
make_histogram 2018 18
make_histogram 2019 19
make_histogram 2020 20
make_histogram 2021 21
do_turnout
exit

