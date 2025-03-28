#!/bin/bash

#
#the SEESV environment variable should be set and needs to point to the directory
#holding the seesv.sh script in RAMADDA's SeeSV  release 
#

set -e
##Ignore and hide any  error in case this is running in a shell that does not have pipefail
set -o pipefail 2>/dev/null || : 

if [ ! -e "${SEESV}/seesv.sh" ]; then
    printf "Error: cannot find ${SEESV}/seesv.sh\nIs the SEESV environment variable set?\n" >&2
fi


seesv() {
    . ${SEESV}/seesv.sh "$@"
}

#usage:
usage() {
    seesv -version
    echo "usage:"
    echo "bash merge.sh <any number of the kbocc csv files> > merged.csv"
    echo "Then add the merged.csv to your RAMADDA as a 'RAMADDA CSV Data' entry type"
}




while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -help)
	    usage
	    exit
	    ;;
        -version)
	    seesv -version
	    exit
	    ;;
	*)
	    break
	    ;;
    esac
done






#merge function is called for each of the .csv files.
#It processes them, converting dates, stripping columns,
#and generating the hours_in_year field

merge() {
    seesv "-delimiter" "?" "-skiplines" "1" "-set" "0" "0" "number" "-set" "1" "0" "Date Time" "-set" "2" "0" "Temperature" "-notcolumns" "0,3-10" "-indateformats" "MM/dd/yy hh:mm:ss a;MM/dd/yyyy HH:mm" "GMT-4" "-outdateformat" "iso8601" "GMT" "-convertdate" "date_time" "-outdateformat" "yyyy-MM-dd HH:mm Z" "UTC" "-indateformat" "iso8601" "GMT" "-extractdate" "date_time" "year" "-extractdate" "date_time" "hours_in_year" "-notcolumns" "date_time" "-lastcolumns" "0"  -print  $1
}




#The tmp.csv will hold all of the processed source files
#we want to skip the header that is written out by the merge call
echo "Year,Hours in year,Temperature" > tmp.csv
for arg in "$@"; do
    if [ ! -e "$arg" ]; then
	echo "File does not exist: $arg"
	exit
    fi
    merge $arg| tail -n +2 >>tmp.csv
done

#Now, tmp.csv is of the form
#year,hours_in_year, temperature
#2018,4207,78.165
#2018,4209,69.685
#...

#but we want to convert it to:
#hours_in_year,2018,2019,2020,2021
#3768,,,41.878,
#...

#the -makefields pivots up the year values and makes them new columns
#then we sort by the hours_in_year
#then we make a formatted field (.e.g, June 15)
#then we add the RAMADDA CSV header, specifying the types of the columns
seesv \
   -makefields year temperature hours_in_year "" \
   -sortby hours_in_year up "" \
   -formatdateoffset hours_in_year hours_in_year \
   -firstcolumns month_day_hour\
   -p tmp.csv 
