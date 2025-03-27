##
##This takes two files of USGS stream gauge downloaded from RAMADDA
##and  creates a merged file with the differences between discharge, volume, etc
##
##usage:
##usgs_makedifference.sh <file1> <file2>
##

csv() {
    sh ${SEESV} "$@"
}

file1="$1"
file2="$2"
base1="${file1%.*}"
base2="${file2%.*}"
named1="tmp_named_${base1}.csv"
named2="tmp_named_${base2}.csv"

if [ -z "$file1" ]; then
    echo "usage: usgs_makedifference.sh <file1> <file2>"
    exit
fi

if [ -z "$file2" ]; then
    echo "usage: usgs_makedifference.sh <file1> <file2>"
    exit
fi


if [ ! -e "${named1}" ]; then
    echo "making ${named1}"
    csv -progress 100 -changerow 0 0-10 "(^.*\$)"  "${base1}_\$1" -p ${base1}.csv > "$named1"
fi

if [ ! -e "${named2}" ]; then
    echo "making ${named2}"
    csv -progress 100 -changerow 0 0-10 "(^.*\$)"  "${base2}_\$1" -p ${base2}.csv > "${named2}"
fi


joined="tmp_joined_${base1}_${base2}.csv"
difference="${base1}_${base2}.csv"

if [ ! -e "${joined}" ]; then
    echo "making $joined"
    csv -progress 100 -join ${base2}_date "${base2}_discharge,${base2}_gauge_height,${base2}_volume,${base2}_total_volume,${base2}_yearly_total_volume" "$named2" ${base1}_date "NaN" -p "$named1" > "$joined"
fi


echo "Making $difference"
csv -progress 100 \
    -operator "${base2}_discharge,${base1}_discharge" discharge_difference "-" \
    -operator "${base2}_volume,${base1}_volume" volume_difference "-" \
    -operator "${base2}_total_volume,${base1}_total_volume" volume_total_difference "-" \
    -operator "${base2}_yearly_total_volume,${base1}_yearly_total_volume" volume_yearly_total_difference "-" \
    -set ${base1}_date 0 date \
    -columns "date,volume_yearly_total_difference,${base1}_yearly_total_volume,${base2}_yearly_total_volume,discharge_difference,volume_difference,${base1}_discharge,${base2}_discharge,${base1}_gauge_height,${base2}_gauge_height,${base1}_volume,${base2}_volume," \
    -p "$joined" > "$difference"


