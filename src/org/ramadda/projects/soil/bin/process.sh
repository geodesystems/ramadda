#!/bin/sh
#################################################
## This processes the data from https://portal.edirepository.org/nis/mapbrowse?scope=edi&identifier=877 
#It cleans up  the file DailyGHG_V1.csv producing dailyghg.csv
#It them samples 0.01% of dailyghg.csv to make a samples.csv
#it then generates a RAMADDA Datatables plugin soil_no2_db.xml which defines the database schema
#################################################

set -e
export mydir=`dirname $0`

export dropdb=0
export install=0

#This calls the SeeSV command line
#The environment variable needs to be set to the seesv.sh
#e.g.
#export SEESV=/Users/jeffmc/bin/seesv.sh

#The -cleaninput says the input CSV is one line per record
#the -dots say to print a progress message

seesv() {
    ${SEESV}  -cleaninput -dots  "tab5000"  "$@"
}


#parse the args
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
	    echo "usage: \n\t-clean\n\t-install\n\t-dropdb <if set this runs RAMADDA locally but tells it to drop the database table. Useful when the schema changes markedly>"
	    exit 1
	    ;;
    esac
done



#lower case the reference column
seesv -case reference lower -p Sitelibrary_V1.csv > sitelibrary.csv

if [ ! -e "dailyghg.csv" ]; then
    #clean up the header, convert the date field, join with the sites, shuffle the columns, etc
    #need to change the year column name because the derby db errors

    echo "creating the dailyghg.csv data file"
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

#Generate the database schema
#The settings are in the file n02db.txt
echo "creating the soil_no2_db.xml database schema"
seesv -db  "file:${mydir}/n02db.txt"  sample.csv > soil_no2_db.xml


#If -install then copy the plugin to the local RAMADDA plugins
if [ "$install" -eq 1 ]; then
    echo "installing the plugin"
    cp soil_no2_db.xml ~/ramadda/plugins
fi

#If dropdb is set (rarely) then run RAMADDA telling it to first drop the table
#This can be used if the schema has markedly changed during development
if [ "$dropdb" -eq 1 ]; then
    echo "running RAMADDA. dropping table"
    sh ~/bin/ramadda.sh -dropdbtable  soil_test1_n02   
fi
