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
	puts stderr "processing $location - $id"
	exec  sh "$::env(SEESV)" -skip 4 -add "location" "$location" -firstcolumns location -p $filename > $processed
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
