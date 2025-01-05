#!/bin/sh
set -e
export mydir=`dirname $0`

if [ ! -d processed ]; then
    mkdir processed
fi

if [ ! -d explode ]; then
    mkdir explode
fi

seesv() {
    ${SEESV}  -cleaninput -dots  "tab5000"  "$@"
}

if [ ! -f explode/aars.csv ]; then
    cd explode
    echo "exploding dailyghg.csv"
    seesv -explode siteid ../dailyghg.csv
    cd ..
fi


process() {
    filename=$(basename "$1")
    echo "processing $filename"
    seesv -split location ";" latitude,longitude \
	  -notcolumns location \
	  -change 9-50 NA NaN \
	  -addheader "siteid.type enumeration site_type.type enumeration treatment.type enumeration crop.type enumeration" \
	  -p $1 > "processed/$filename"
}

for file in explode/*.csv
do
    process "$file"
done


