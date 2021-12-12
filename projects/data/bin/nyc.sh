#!/bin/sh
export mydir=`dirname $0`
export data=data
#export csv=~/bin/csv.sh 

csv() {
    ~/bin/csv.sh -cleaninput -dots 100000 "$@"
}



dof=${data}/DOF__Property_Charges_Balance.csv
nyc=${data}/NYC_Citywide_Annualized_Calendar_Sales_Update.csv
prop=${data}/Property_Valuation_and_Assessment_Data_Tax_Classes_1_2_3_4.csv

joined1=${data}/joined1.csv
joined2=${data}/nyc_property_joined.zip
dof_pruned=${data}/dof_pruned.csv
prop_pruned=${data}/prop_pruned.csv
nyc_pruned=${data}/nyc_pruned.csv
nyc_pruned_columns="zip_code,latitude,longitude,community_board,council_district,address,borough,sale_price,sale_date"
prop_pruned_columns=",fintxbtot,curmkttot,zip_code,bldg_class,zoning"

clean() {
    rm -f ${dof_pruned}
    rm -f ${nyc_pruned}
    rm -f ${prop_pruned}
    rm -f ${joined1}
    rm -f ${joined2}        
}

clean


if [ ! -f ${nyc_pruned} ]; then
    echo "making ${nyc_pruned}"
    csv  -set ease_ment 0 easement -headerids \
	 -padleft borough 0 1 -padleft block 0 5 -padleft lot 0 4  \
	 -combine "borough,block,lot" "" parid \
	 -latest parid sale_date "MM/dd/yyyy" \
	 -c "parid,${nyc_pruned_columns}" \
	 -p ${nyc} > ${nyc_pruned}
fi


if [ ! -f ${dof_pruned} ]; then
    echo "making ${dof_pruned}"
    csv   -maxrows 1000000  -headerids -lt valclass 3 -gt valclass 0 -c parid,valclass,LUC,Sum_bal,Sum_coll,Sum_dsc,sum_liab -p ${dof} > ${dof_pruned}
fi


if [ ! -f ${prop_pruned} ]; then
    echo "making ${prop_pruned}"
    csv  -headerids -maxrows 10000000 -c "parid,${prop_pruned_columns}" -p ${prop} > ${prop_pruned}
fi

rm -f ${joined2}
if [ ! -f ${joined1} ]; then
    echo "making ${joined1}"
    csv  -join parid ${nyc_pruned_columns} ${nyc_pruned}  parid XXXX1 -output ${joined1} -p ${dof_pruned} 
fi

rm -f ${joined2}

if [ ! -f ${joined2} ]; then
    echo "making ${joined2}"
    csv -join parid ${prop_pruned_columns} ${prop_pruned}  parid XXXX2 -output ${joined2} -p ${joined1}
fi

exit




