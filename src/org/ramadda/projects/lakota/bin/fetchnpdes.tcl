if {0} {
    #grab the html file from the epa that lists the npdes permits-
    https://www.epa.gov/npdes-permits/south-dakota-npdes-permits
    
    #run the seesv script to extract the data from the html
    #generate a tcl script that we source
    seesv -htmltable 0 "" "" -tcl site epa.html > epasites.tcl

    #the script below then runs through each site and downloads the data file from:
    https://echo.epa.gov/trends/loading-tool/get-data/monitoring-data-download

    #It then runs the seesv to strip out extraneous headers and it inserts the location and facility name

    #It also combines the parameter_description, statistical_base and outfall_number columns into a single parameter column

    #It then reorders the columns and drops extraneous columns

    #It then cleans up the non-numeric values in the  limit_value and  dmr_value columns

    #It then runs through all of the processed site csv files and merges them into a single file 

}


set ::siteArgs { location  facility_or_permit_name  permit_number  permit_status  effective_date  expiration_date }
proc site $::siteArgs {
    set id $permit_number
    set filename "$id.csv"
    if {![file exists $filename]} {
	catch {
	    puts "fetching $filename"
	    exec wget -O "$filename"   "https://echodata.epa.gov/echo/dmr_rest_services.get_monitoring_data_csv?p_start_date=05/2010&p_end_date=03/2025&p_npdes_id=$id&output=CSV"
	}
    }

    set processed processed_${id}.csv
    if {![file exists $processed]} {
	puts stderr "processing $facility_or_permit_name - $id"
	exec  sh "$::env(SEESV)" -skip 4 -combine "parameter_description,statistical_base,outfall_number" " - " parameter  -add "location" "$location" -add "facility" "$facility_or_permit_name" -firstcolumns "location,facility" -firstcolumns "location,facility,parameter"  -notcolumns "outfall_number,monitoring_location_code,limit_set_designator,parameter_code,parameter_description,statistical_base" -change limit_value Mon NaN -change dmr_value "^NODI:.*$" NaN -change dmr_value ".*<.*" NaN -p $filename > $processed
    }
}

source epasites.tcl

set cnt 0
set combined sd_npdes_epa.csv
foreach file [glob processed_*.csv] {
    incr cnt
    if {$cnt==1} {
	exec cat $file > $combined
    } else {
	exec tail -n +2 $file >> $combined
    }
}
