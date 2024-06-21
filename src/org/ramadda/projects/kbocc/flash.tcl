#This downloads the flash grib files
proc fetch {day hour} {
    set file "maxstreamflow_${day}-${hour}.grib2.gz"
    set suffix "MRMS_FLASH_CREST_MAXSTREAMFLOW_00.00_${day}-${hour}.grib2.gz"    
    if {[file exists $file]} {
	if {[file size $file]>2000000} return
	file delete $file
    }
    puts stderr "fetching $day $hour"
    catch {exec wget -O $file "https://noaa-mrms-pds.s3.amazonaws.com/CONUS/FLASH_CREST_MAXSTREAMFLOW_00.00/${day}/$suffix"}
    if {[file size $file]<1000} {
	file delete $file
    }
}

proc download {days hours} {
    foreach day $days {
	foreach hour $hours {
	    fetch $day $hour
	}
    }
}


set days {20240601  20240602 20240603 20240604 20240605 20240606 20240607 20240608 20240609 20240610 20240611 20240612 20240613 20240614 20240615 20240616 20240617 20240618 20240619 20240620}
set hours {000000 120000}

set hourlyHours {}
set days {20240619 20240620}
for {set hour 0} {$hour<24} {incr hour} {
    set h $hour
    if {$h<=9} {set  h "0$h"}
    lappend  hourlyHours "${h}0000"
}

download $days $hourlyHours

#download $days $hours
