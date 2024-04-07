#!/bin/bash
set -e

#########################################################
## Requirements
#########################################################

#This script needs the RAMADDA SeeSV package installed
#Download and unzip:
#https://ramadda.org/repository/release/latest/seesv.zip
#You need to have Java installed
#Consult the README in the seesv directory
#Set the environment variable SEESV to the seesv directory



#You can specify the the 3 flood levels (minor, moderate, major) and the datum
#You need to specify the station - consult https://tidesandcurrents.noaa.gov/map/index.html
#To run:
#sh gettide.sh -help to see usage



#########################################################
# Output
#########################################################

#This script downloads the hourly tide data for a given station from https://tidesandcurrents.noaa.gov
#Each file is downloaded to a station_<station> directory
#It then produces:

#The merged data for all of the years
#hourly_height_<station>.csv

#A count of the hours above flood stage per year
#hours_above_flood_stage_<station>.csv

#The dates of the flood events where a flood event is where the flood stage is above
#moderate flood level. The events delimited by at at least 2 days between the high water
#flood_events_<station>.csv




csv() {
    sh ${SEESV}/seesv.sh "$@"
}


fetch() {
    year=$1
    station=$2
    file=$3
    if [ ! -e "$file" ]; then
	echo "fetching $year"
	url="https://api.tidesandcurrents.noaa.gov/api/prod/datagetter?product=${product}&begin_date=${year}0101&end_date=${year}1230&datum=${datum}&station=${station}&time_zone=${timezone}&units=english&format=csv&application=$app"
	echo $url
	wget --verbose  -O $file "$url"
    fi
}

countHours() {
    station="$1"
    file="$2"
    floodlevel="$3"
    floodlevelName="$4"
    csv -dots 10000 "-dateformat" "yyyy-MM-dd HH:mm" "GMT" "-extractdate" "date_time" "year" "-ge" "water_level" "$floodlevel" "-summary" "year" "water_level" "" "count" -set 1 0 "Hours above ${floodlevelName} - $floodlevel ft" \
       -p  $file > hours_above_flood_stage_${floodlevel}_${station}.csv
}


processStation() {
    station=$1
    dir=station_${station}
    mkdir -p $dir
    all=${product}_$station.csv
    rm -f $all
    for year in {1930..2023}; do
	file="${dir}/data_${product}_${station}_${year}.csv"
	fetch $year $station $file
	# Check if the file exists and is a regular file
	size=$(wc -c < "$file")
	if [ "$size" -gt 500 ]; then
	    if [ ! -e $all ]; then
		cat "$file" > $all
	    else
		tail -n +2 "$file" >> $all
	    fi
	fi
    done

    echo "Counting flood events: flood_events_${station}.csv"
    csv    -dots 10000 -indateformat "yyyy-MM-dd HH:mm"  GMT \
	   -ge water_level $level2 \
	   -elapsed date_time   -msto elapsed days \
	   -ge days 2 \
	   -p $all > flood_events_${station}.csv

    echo "counting hours of flood events per year"
    countHours $station $all $level1 "Minor Flood Stage"
    countHours $station $all $level2 "Moderate Flood Stage"
    countHours $station $all $level3 "Major Flood Stage"
    echo "merging file: hours_above_flood_stage_${station}.csv"
    csv  -join year 1 hours_above_flood_stage_${level2}_${station}.csv year NaN \
       -p hours_above_flood_stage_${level1}_${station}.csv >tmp.csv
    csv  -join year 1 hours_above_flood_stage_${level3}_${station}.csv year NaN \
       -p tmp.csv > hours_above_flood_stage_${station}.csv

    #cleanup
    rm hours_above_flood_stage_${level1}_${station}.csv 
    rm hours_above_flood_stage_${level2}_${station}.csv
    rm hours_above_flood_stage_${level3}_${station}.csv 
    rm tmp.csv    

}




datum=MLLW
timezone=GMT
app=NOS.COOPS.TAC.WL
product=hourly_height
level1=2.6
level2=3
level3=4
station=8575512 

usage() {
    printf "Usage:\n gettide.sh -l <minor> <moderate> <major> -datum <some datum> <station>\n"
    exit
}

while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -help)
	    usage
	    ;;
        -level)
	    shift
	    level1=$1
	    shift
	    level2=$1
	    shift
	    level3=$1
	    shift
            ;;
	-datum)
	    shift
	    datum=$1
	    shift
	    ;;
	*)
	    station=$1
	    shift
	    ;;
    esac
done


echo "processing: station:$station datum:$datum levels:$level1 $level2 $level3"
processStation "$station"





