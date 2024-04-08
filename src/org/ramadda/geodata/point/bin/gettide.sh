#!/bin/bash
set -e

#########################################################
## Requirements
#########################################################

#This script needs the RAMADDA SeeSV package installed
#Download and unzip: https://ramadda.org/repository/release/latest/seesv.zip
#You need to have Java installed - consult the README in the seesv directory
#To run the script set the environment variable SEESV to the seesv directory


#########################################################
# Usage
#########################################################

#You can specify the 3 flood levels (minor, moderate, major) and the datum
#You need to specify the station - consult https://tidesandcurrents.noaa.gov/map/index.html
#To run:
#sh gettide.sh -help to see usage
#-level and -datum are optional
#datum defaults to STND
#sh gettide.sh -level <minor> <moderate> <major> -datum <some datum> <station>



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

#The counts per year of the flood events
#flood_events_counts_8575512.csv 

datum=STND
level1=
level2=
level3=
station=8575512 



csv() {
    sh ${SEESV}/seesv.sh "$@"
}


fetch() {
    year=$1
    station=$2
    file=$3
    product=hourly_height
    timezone=GMT
    if [ ! -e "$file" ]; then
	echo "fetching $year"
	url="https://api.tidesandcurrents.noaa.gov/api/prod/datagetter?product=${product}&begin_date=${year}0101&end_date=${year}1230&datum=${datum}&station=${station}&time_zone=${timezone}&units=english&format=csv&application=$NOS.COOPS.TAC.WL"
	echo $url
	wget --verbose  -O $file "$url"
    fi
}

setLevels() {
    station=$1
    if [ ! -e "floodlevels_$station.json" ]; then
	echo "Fetching flood levels"
	wget --verbose  -O floodlevels_$station.json "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/$station/floodlevels.json"
    fi
    level1=$(awk -F'[:,]' '/"nos_minor"/ {print $2}' floodlevels_$station.json)
    export level1=$(echo $level1 | tr -d ' ')
    level2=$(awk -F'[:,]' '/"nos_moderate"/ {print $2}' floodlevels_$station.json)
    export level2=$(echo $level2 | tr -d ' ')
    level3=$(awk -F'[:,]' '/"nos_major"/ {print $2}' floodlevels_$station.json)
    export level3=$(echo $level3 | tr -d ' ')        
    echo "level:$level1 $level2 $level3"
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
    all=heights_$station.csv
    rm -f $all
    for year in {1930..2023}; do
	file="${dir}/data_${product}_${datum}_${station}_${year}.csv"
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

    csv -extractdate date_time year -summary year water_level "" count \
	-p flood_events_${station}.csv > flood_events_counts_${station}.csv


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
    rm hours_above_flood_stage_*_${station}.csv 
    rm tmp.csv    

}







usage() {
    printf "Usage:\n gettide.sh -level <minor> <moderate> <major> -datum <some datum> <station>\n"
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
	    if [[ $station == -* ]]; then
		echo "Bad argument: $station"
		usage
	    fi
	    shift
	    ;;
    esac
done


if [ -z "$level1" ]; then
    setLevels $station
fi




echo "processing: station:$station datum:$datum levels:$level1 $level2 $level3"
processStation "$station"





