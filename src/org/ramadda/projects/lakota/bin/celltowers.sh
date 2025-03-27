#download the ftp://wirelessftp.fcc.gov/pub/uls/complete/r_tower.zip

csv() {
    sh ${SEESV} "$@"
}


if [ ! -e "ra.csv" ]; then
    csv -delimiter "|" \
	-progress 1000 \
	-header "Record Type,Content Indicator,File Number,Registration Number,Unique System Identifier,Application Purpose,Previous Purpose,Input Source Code,Status Code,Date Entered,Date Received,Date Issued,Date Constructed,Date Dismantled,Date Action,Archive Flag Code,Version,Signature First Name,Signature Middle Initial,Signature Last Name,Signature Suffix,Signature Title,Invalid Signature,Structure_Street Address,Structure_City,Structure_State Code,County Code,ZIP Code,Height of Structure,Ground Elevation,Overall Height Above Ground,Overall Height AMSL,Structure Type,Date FAA Determination Issued,FAA Study Number,FAA Circular Number,Specification Option,Painting and Lighting,Proposed Marking and Lighting,Marking and Lighting Other,FAA EMI Flag,NEPA Flag,Date Signed,Assignor Signature Last Name,Assignor Signature First Name,Assignor Signature Middle Initial,Assignor Signature Suffix,Assignor Signature Title,Assignor Date Signed" \
	-p ra.dat > ra.csv
fi



if [ ! -e "en.csv" ]; then
    echo "making en.csv"
    csv -delimiter "|" \
	-progress 1000 \
	-header "Record Type,Content Indicator,File Number,Registration Number,Unique System Identifier,Contact Type,Entity Type,Entity Type - Other,Licensee ID,Entity Name,First Name,MI,Last Name,Suffix,Phone,Fax Number,Internet Address,Street Address,Street Address 2,PO Box,City,State,Zip Code,Attention,FRN" \
	-p EN.dat > en.csv
fi


if [ ! -e "co.csv" ]; then
    echo "making co.csv"
    csv -delimiter "|" \
	-progress 1000 \
	-header "Record Type,Content Indicator,File Number,Registration Number,Unique System Identifier,Coordinate Type,Latitude Degrees,Latitude Minutes,Latitude Seconds,Latitude Direction,Latitude_Total_Seconds,Longitude Degrees,Longitude Minutes,Longitude Seconds,Longitude Direction,Longitude Total Seconds,Array Tower Position,Array Total Tower" \
	-scale "Latitude_Total_Seconds" 0 0.00027777777 0 \
	-scale "Longitude_Total_Seconds" 0 -0.00027777777 0 \
	-columns "registration_number,Latitude_Total_Seconds,Longitude_Total_Seconds" \
	-set Latitude_Total_Seconds 0 latitude -set  Longitude_Total_Seconds 0 longitude \
	-p CO.dat > co.csv
fi

if [ ! -e "merged.csv" ]; then
    echo "making merged.csv"
    csv     -progress 1000 \
	    -join registration_number "structure_city,structure_state_code,overall_height_above_ground,overall_height_amsl,structure_type" ra.csv  registration_number "" \
	    -join registration_number "entity_name,internet_address" en.csv  registration_number "" \
	    -p co.csv > merged.csv
fi

csv -match structure_state_code "(SD)" \
    -addheader "structure_state.type enumeration  structure_type.type enumeration" \
    -p merged.csv > sd_antennas.csv
