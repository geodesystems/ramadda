set urls {
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-WH-R-COTTONWOOD_01
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-WH-R-WHITE_04
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-WH-R-LITTLE_WHITE_01
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-WH-R-WHITE_02
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-MI-R-MEDICINE_01
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-CH-R-BEAVER_01_USGS
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-CH-R-CHEYENNE_04
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-MI-R-FRANCIS_CASE_01
    https://apps.sd.gov/NR92WQMAP/WQDashboard/Stream/SD-CH-R-GRACE_COOLIDGE_01
}





source $env(RAMADDA_ROOT)/bin/ramadda.tcl


proc readFile {f} {
    set fp [open $f r]
    set c [read $fp]
    close $fp
    set c
}

proc writeFile {f c} {
    set fp [open $f w]
    puts $fp $c
    close $fp
}



set ::xml "<entries>\n"
foreach url $urls {
    set site [file tail $url]
    set f [file join html $site]
    if {![file exists $f]} {
	puts stderr "fetching $url as $f"
	catch {exec wget -O $f $url} err
#	puts $err
    }
    set locf [file join html ${site}_loc.html]
    if {![file exists $locf]} {
	regsub -all WQDashboard $url MonitoringData locurl
	puts stderr "fetching $locurl as $locf"
	catch {exec wget -O $locf $locurl} err
#	puts $err
    }



    set c [readFile $locf]
    regexp {<td>(-?\d+\.\d+)</td>\s*<td>(-?\d+\.\d+)</td>} $c -> latitude longitude
    set c [readFile $f]
    regexp {modelParameters *= *([^;]+);} $c match json
    set jsonFile [file join data [file tail $url].json]
    set fp [open $jsonFile w]
    puts $fp $json
    close $fp

    set name $site
    append ::xml [openEntry type_sdwq  {} {} "$name" latitude $latitude longitude $longitude file $jsonFile]
    set desc "+credit\nData is from the South Dakota  \[<_sdwq>$url DANR Water Quality MAP\] site\n-credit"
    append ::xml [col description $desc]
    append ::xml [col auid $site]	
    append ::xml [closeEntry] 
}

append ::xml "</entries>\n";
writeFile entries.xml $::xml

