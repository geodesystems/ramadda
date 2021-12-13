#!/bin/sh
export mydir=`dirname $0`
set -e

#This assumes the directory you are in has a data sub-directory  that holds
#the source files from
#https://data.cityofnewyork.us/City-Government/Property-Valuation-and-Assessment-Data-Tax-Classes/8y4t-faws
#https://data.cityofnewyork.us/City-Government/DOF-Property-Charges-Balance/scjx-j6np
#https://data.cityofnewyork.us/City-Government/NYC-Citywide-Annualized-Calendar-Sales-Update/w2pb-icbu
export data=data

#missing values in join
missing1=NaN
missing2=NaN



#max rows to process - some really big number
#as the dof file 43 million rows
#set this to some small number with -maxrows 10000 for testing
maxrows=100000000

usage() {
    echo "nyc.sh <-help> <-clean> <-maxrows 10000>"
}


#These variables are short hand references to the source files
dof=${data}/DOF__Property_Charges_Balance.csv
nyc=${data}/NYC_Citywide_Annualized_Calendar_Sales_Update.csv
prop=${data}/Property_Valuation_and_Assessment_Data_Tax_Classes_1_2_3_4.csv

#These are the cleaned up files
dof_pruned=${data}/dof_pruned.csv
prop_pruned=${data}/prop_pruned.csv
nyc_pruned=${data}/nyc_pruned.csv

#The columns we extract from ncy
nyc_pruned_columns="zip_code,latitude,longitude,community_board,council_district,address,borough,sale_price,sale_date"

#The columns we extract from prop
prop_pruned_columns=",fintxbtot,curmkttot,zip_code,bldg_class,zoning"


#The first temporary file joing dof with nyc
joined1=${data}/joined1.csv

#the final file
joined2=${data}/nyc_property_joined.zip


#This does the processing
#To use this from your own csv install change the ~/bin/csv.sh
#to point to your install location
csv() {
    #The -cleaninput says that the data is well behaved with each data row
    #on one line, etc
    #the -dots says to print out how many rows have been processed
    ~/bin/csv.sh -cleaninput -dots 100000 "$@"
}



#cleans up all of the derived files
clean() {
    rm -f ${dof_pruned}
    rm -f ${nyc_pruned}
    rm -f ${prop_pruned}
    rm -f ${joined1}
    rm -f ${joined2}        
}


while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -clean)
	    clean
	    shift
            ;;
        -maxrows)
	    shift
            maxrows="$1"
	    shift
            ;;
	-help)
	    usage
	    exit 1
	    ;;
	*)
	    echo "Unknown argument:$arg"
	    usage
	    exit 1
	    ;;
    esac
done



#generate the pruned nyc file if needed
if [ ! -f ${nyc_pruned} ]; then
    echo "making ${nyc_pruned}"
    #This says to:
    #-set: change the name of the header ease_ment to easement
    #-headerids: clean up the header names
    #-padleft: pad out the columns with "0" to the specified length
    #-combine: combine the columns to form the new parid column
    #-latest: extract out the latest date, grouping by parid
    #-c: pass through the columns
    #-p: print as csv
    csv  -maxrows ${maxrows} \
	 -set ease_ment 0 easement -headerids \
	 -padleft borough 0 1 \
	 -padleft block 0 5 \
	 -padleft lot 0 4  \
	 -combine "borough,block,lot" "" parid \
	 -latest parid sale_date "MM/dd/yyyy" \
	 -c "parid,${nyc_pruned_columns}" \
	 -p ${nyc} > ${nyc_pruned}
fi



if [ ! -f ${prop_pruned} ]; then
    echo "making ${prop_pruned}"
#clean up header names, limit max rows produced, print out columns
    csv  -headerids -maxrows ${maxrows} \
	 -c "parid,${prop_pruned_columns}" \
	 -p ${prop} > ${prop_pruned}
fi



if [ ! -f ${dof_pruned} ]; then
    echo "making ${dof_pruned}"
#-maxrows: only process maxrows number of rows
#-headerids: clean up the header names
#-pattern: only pass through rows with valclass matches the regexp
#-c: select columns
    csv   -maxrows ${maxrows}  -headerids \
	  -pattern valclass "^(1|2)$" \
	  -ifin parid ${nyc_pruned} parid \
	  -ifin parid ${prop_pruned} parid \
	  -c parid,valclass,luc,sum_bal,sum_coll,sum_dsc,sum_liab \
	  -p ${dof} > ${dof_pruned}
fi




if [ ! -f ${joined1} ]; then
    echo "making ${joined1}"
    #join the files
    #This joins dof_pruned (left side) with the rows (if found) in nyc_pruned (right side) file
    #the format is: -join <column in right to match on> <columns in right to include> <right file> <column in left to match on> <missing_value>
    csv  -join parid ${nyc_pruned_columns} ${nyc_pruned}  parid ${missing1} -output ${joined1} -p ${dof_pruned} 
fi

if [ ! -f ${joined2} ]; then
    echo "making ${joined2}"
#Now joins the temporary file with the data in prop_pruned
    csv -join parid ${prop_pruned_columns} ${prop_pruned}  parid ${missing2} -output ${joined2} -p ${joined1}
fi

echo "making the records"
echo "Records:" > records.txt

echo "\n***********\n${nyc_pruned}" >> records.txt
csv -maxrows 2 -record $nyc_pruned>>records.txt

echo "\n***********\n${prop_pruned}" >> records.txt
csv -maxrows 2 -record $prop_pruned>>records.txt

echo "\n***********\n${dof_pruned}" >> records.txt
csv -maxrows 2 -record $dof_pruned>>records.txt


echo "\n***********\n${joined1}" >> records.txt
csv -maxrows 2 -record $joined1>>records.txt

echo "\n***********\n${joined2}" >> records.txt
csv -maxrows 2 -record $joined2>>records.txt


echo "done"






