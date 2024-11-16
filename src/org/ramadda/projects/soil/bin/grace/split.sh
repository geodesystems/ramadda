#!/bin/sh
set -e
export mydir=`dirname $0`


#This calls the SeeSV command line
#The environment variable needs to be set to the seesv.sh
#e.g.
#export SEESV=/Users/jeffmc/bin/seesv.sh

#The -cleaninput says the input CSV is one line per record
#the -dots say to print a progress message

seesv() {
    ${SEESV}   -dots  "tab2000"  "$@"
}

files=("measargenes" "measbiomasscho" "measbiomassenergy" "measbiomassminan" "measgasnutrientloss" "measghgflux" "measgrazingplants" "measharvestfraction" "measnutreff" "measresiduemgnt" "meassoilbiol" "meassoilchem" "meassoilcover" "meassoilphys" "measwaterqualityarea" "measwaterqualityconc" "measwinderosionarea" "measyieldnutuptake" "mgtamendments" "mgtgrazing" "mgtgrowthstages" "mgtplanting" "mgtresidue" "mgttillage")



for file in "${files[@]}"; do
    if [ ! -d "$file" ]; then
	mkdir "$file"
    fi
    echo "Processing: $file"
    cd $file
    seesv -explode siteid "${file}_\${value}.csv" ../../exploded/$file.csv
    cd ..
done

