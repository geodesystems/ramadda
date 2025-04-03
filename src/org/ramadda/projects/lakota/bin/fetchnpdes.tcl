set ::seesv "$::env(SEESV)"
set ::echofile echo/sdecho.csv

set ::combined sd_npdes_epa.csv
set ::joined epa_joined.csv
set ::plugin epa_npdes_db.xml

set ::clean 1

if {$::clean} {
#    catch {exec rm $::combined}
    catch {exec rm $::joined}    
}


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

    #It then runs through all of the processed site csv files and merges them into a single file 

}


proc site {id} {
    set filename "download/$id.csv"
    if {![file exists $filename]} {
	catch {
	    puts "fetching $filename"
	    exec wget -O "$filename"   "https://echodata.epa.gov/echo/dmr_rest_services.get_monitoring_data_csv?p_start_date=05/2010&p_end_date=03/2025&p_npdes_id=$id&output=CSV"
	}
    }

    set processed processed/processed_${id}.csv
    if {![file exists $processed]} {
	puts stderr "processing  $processed"
	exec  sh "$::seesv" -skip 4 -combine "parameter_description,statistical_base,outfall_number" " - " full_parameter  -firstcolumns "full_parameter"    -p $filename > $processed
##-notcolumns "outfall_number,monitoring_location_code,limit_set_designator,parameter_code,parameter_description,statistical_base"
    }
}


source sdids.tcl
foreach id $::ids {
    site $id
}


if {![file exists $::combined]} {
    puts stderr "creating the merged file $::combined"
    set cnt 0
    foreach file [glob processed/processed_*.csv] {
	if {[file size $file]==0} {
	    continue;
	}
	incr cnt
	if {$cnt==1} {
	    exec cat $file > $::combined
	} else {
	    exec tail -n +2 $file >> $::combined
	}
    }
    puts stderr "#files: $cnt"
}


if {![file exists $::joined]} {
    puts stderr "making $::joined"
    exec  sh $::seesv  -notmatch 0 SD0020699 -change limit_value Mon NaN -change limit_value Mon NaN  -trim dmr_value -change dmr_value "^\$" NaN  -change dmr_value "E " "" -change dmr_value ".*<.*" 0 -change dmr_value "^NODI:.*$" NaN -change dmr_value ".*(<|>).*" NaN -setting join.split " " -join NPDES_IDS "fac_name,fac_city,fac_state,fac_county,fac_lat,fac_long" "$::echofile" npdes_permit_number ""   -indateformats "MM/dd/yyyy" "UTC" -convertdate "monitoring_period_date" -set fac_name 0 "Facility Name" -set fac_city 0 City -set fac_state 0 State -set fac_county 0 County -set fac_lat 0 latitude -set fac_long 0 longitude  -combine "latitude,longitude" ";" location -drop "latitude,longitude" -firstcolumns "facility_name,npdes_permit_number,city,state,county,location,full_parameter,dmr_value,parameter_code,parameter_description,statistical_base,outfall_number" -p $::combined > $::joined
}

puts stderr "making db schema $:plugin"
exec sh $::seesv   -setting db.comment "This is a generated plugin file for the EPA NPDES database" -db "file:db.properties" $::joined > $::plugin
