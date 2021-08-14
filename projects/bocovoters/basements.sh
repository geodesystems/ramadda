#!/bin/sh
set -e
export csv=~/bin/csv.sh

echo "cleaning up addresses"
${csv} -change main_address " BL$" " BLVD" \
       -change main_address " AV$" " AVE" \
       -change main_address " PY$" " PKWY" \
       -change main_address " WY$" " WAY" \
       -change main_address " CR$" " CIR" \
       -change main_address WILLOWBROOK "WILLOW BROOK" \
       -split location "|" latitude,longitude \
       -notcolumns location \
       -p rental.csv > rental_full.csv

echo "making boulder owners"
${csv} -pattern city BOULDER -columns strap,owner_name,str_num,str,str_pfx,str_sfx -concat str_num,str_pfx,str,str_sfx " " "Full address" -trim full_address -change full_address "  +" " " -p Owner_Address.csv  > boulderowners.csv

echo "making unfinished basements"
${csv} -pattern 15 "(BGU|BSU|BWU|LGU|LWU)" -columns strap,bsmtsf -p Buildings.csv  > basements.csv

echo "summing sq ft"
${csv} -sum strap bsmtsf "" -p basements.csv > basementsums.csv


${csv} -unique strap -columns strap,nbrBedRoom -set nbrBedRoom 0 "number bedrooms" -change number_bedrooms "\..*$" "" -p Buildings.csv  > bedrooms.csv


echo "joining"
${csv} -join 0 1 basementsums.csv strap 0 -join 0 1 bedrooms.csv strap 0 -p boulderowners.csv > bedrooms_basement.csv


${csv} -join full_address  bsmtsf,number_bedrooms bedrooms_basement.csv main_address -999  -p rental_full.csv  > results.csv

grep "\-999" results.csv > nomatch.csv
grep -v "\-999" results.csv > match.csv
${csv} -addheader "rental_type.type enumeration" -p match.csv  >  rental_bedrooms_basement.csv
wc -l nomatch.csv
wc -l match.csv


