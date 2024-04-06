



datum=MLLW
timezone=GMT
app=NOS.COOPS.TAC.WL

fetch() {
    year=$1
    station=$2
    file=$3
    if [ ! -e "$file" ]; then
	echo "fetching $year"
	url="https://api.tidesandcurrents.noaa.gov/api/prod/datagetter?product=high_low&begin_date=${year}0101&end_date=${year}1230&datum=${datum}&station=${station}&time_zone=${timezone}&units=english&format=csv&application=$app"
	echo $url
	wget --verbose  -O $file "$url"
    fi
}

processStation() {
    station=$1
    dir=station_${station}
    mkdir -p $dir
    all=all$station.csv
    rm -f $all
    for year in {1930..2023}; do
	file="${dir}/data_${station}_${year}.csv"
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
}



for arg in "$@"
do
    echo "processing $arg"
    processStation "$arg"
done


#processStation 8571421




