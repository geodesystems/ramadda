#!/bin/sh
set -e
export mydir=`dirname $0`

export dots=5000
export dropdb=0
export install=0

seesv() {
    ${SEESV}  -cleaninput -dots  "tab${dots}"  "$@"
}



while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
	-clean)
	    echo 'cleaning'
	    rm -r -f dailyghg.csv
	    rm -r -f sample.csv	    
	    shift
	    ;;
	-install)
	    install=1
	    shift
	    ;;	
	-dropdb)
	    dropdb=1
	    shift
	    ;;	
	*)
	    echo "Unknown argument:$arg"
	    echo "usage: \n\t-clean"
	    exit 1
	    ;;
    esac
done



#lower case the reference column
seesv -case reference lower -p Sitelibrary_V1.csv > sitelibrary.csv

if [ ! -e "dailyghg.csv" ]; then
    #clean up the header, convert the date field, join with the sites, shuffle the columns, etc
    #need to change the year column name because the derby db errors
    seesv -headerids \
	  -case siteid lower \
	  -set year 0 "observation_year"  -set month 0 "observation_month" \
	  -indateformats MM/dd/yyyy UTC \
	  -convertdate date \
	  -join reference latitude,longitude sitelibrary.csv siteid NaN \
	  -firstcolumns siteid,date,observation_year,observation_month \
	  -columnsafter tavg "wind,wind_max" \
	  -combine "latitude,longitude" ";" Location \
	  -notcolumns latitude,longitude \
	  -columnsafter observation_month location \
	  -p DailyGHG_V1.csv > dailyghg.csv
fi

if [ ! -e "sample.csv" ]; then
    seesv -sample 0.01 -p dailyghg.csv > sample.csv
fi

seesv -db  "file:${mydir}/n02db.txt"  sample.csv > soil_no2_db.xml


if [ "$install" -eq 1 ]; then
    echo "installing the plugin"
    cp soil_no2_db.xml ~/ramadda/plugins
fi

if [ "$dropdb" -eq 1 ]; then
    echo "running RAMADDA. dropping table"
    sh ~/bin/ramadda.sh -dropdbtable  soil_test1_n02   
fi
