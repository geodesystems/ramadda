
datum=MLLW
timezone=GMT
app=NOS.COOPS.TAC.WL
product=hourly_height
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
    sh ${SEESV}/seesv.sh -dots 10000 "-dateformat" "yyyy-MM-dd HH:mm" "GMT" "-extractdate" "date_time" "year" "-ge" "water_level" "$floodlevel" "-summary" "year" "water_level" "" "count" -set 1 0 "Hours above ${floodlevelName} - $floodlevel ft" \
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
    countHours $station $all 2.6 "Minor Flood Stage"
    countHours $station $all 3.3 "Moderate Flood Stage"
    sh ${SEESV}/seesv.sh  -join year 1 hours_above_flood_stage_3.3_${station}.csv year NaN \
       -p hours_above_flood_stage_2.6_${station}.csv > hours_above_flood_stage_${station}.csv
}



for arg in "$@"
do
    echo "processing $arg"
    processStation "$arg"
done


#processStation 8571421




