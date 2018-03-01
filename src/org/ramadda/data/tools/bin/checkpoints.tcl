set ::dev /archive/dev
set ::dev /data/archive/dev
set ::boundssh $::dev/nlastools/lidarbounds.sh
proc writeFile {file contents} {
    set fp [open $file w]
    puts $fp $contents
    close $fp
}

#Recurse down the tree
proc walkTree {file {crs {}} {utmZone {}} {utmNorth {}}} {
    set tail [file tail $file]
    set destPropertiesFile [file join $file point.properties]
    if {[file isdirectory $file]} {
	if {$tail == "ECEF"} {
	    set crs ecef
	} elseif {[regexp {.*Lat.*Lon} $tail]} {
	    set crs latlon
	} elseif {[regexp {UTM-([0-9]+)(S|N)} $tail match zone hemisphere]} {
	    set utmZone $zone
	    set utmNorth true
	    if  {$hemisphere == "S"} {
		set utmNorth false
	    }
	    set crs utm
	}
    }

    if {[file isdirectory $file]} {
	foreach child [glob -nocomplain $file/*] {
	    #	puts "file: $child"
	    walkTree $child $crs $utmZone $utmNorth
	}
	return
    }

    if {([regexp {\.txt$} $file] || [regexp {\.csv$} $file]) && $crs!=""} {
	if {[regexp -nocase {(coord|target)} $file]} {
	    return;
	}

	set parent [file dirname $file]
	#Check if there is a point.properties file here
	set  propertiesFile [file join $parent point.properties]
	set delimiter ","
	set contents ""
	switch $crs {
	    ecef {
		set contents [read [open $::dev/ecef_point.properties r]]
	    }
	    latlon {
		set contents [read [open $::dev/latlon_point.properties r]]
	    }
	    utm {
		set contents [read [open $::dev/utm_point.properties r]]
		regsub -all {{utm.zone}} $contents $utmZone contents
		regsub -all {{utm.north}} $contents $utmNorth contents
	    }
	}
	
	if {$contents==""} {
	    puts "Error: could not determine CRS for: $file"
	    return
	}

	set hadExisted [file exists $propertiesFile]

	if {!$hadExisted} {
	    puts "writing property file: $propertiesFile"
	    regsub -all {{delimiter}} $contents $delimiter converted
	    writeFile $propertiesFile  $converted
	}

	if {![catch {exec sh $::boundssh -max 10 $file} err]} {
	    puts "OK: [file tail $file]"
	    return;
	}

	if {!$hadExisted} {
	    puts "Error: file could not be read:  $file]"
	    return;
	}

#	puts "Trying out space delimiter"
	regsub -all {{delimiter}} $contents {}  converted
	writeFile $propertiesFile  $converted
	if {![catch {exec sh $::boundssh -max 10 $file} err]} {
	    puts "OK: [file tail $file]"
	    return;
	}

#	puts "Trying tab delimiter"
	regsub -all {{delimiter}} $contents {\\t}  converted
	writeFile $propertiesFile  $converted
	if {![catch {exec sh $::boundssh -max 10 $file} err]} {
	    puts "OK: [file tail $file]"
	    return;
	}

	puts "Removing the intesity field: $file"
	regsub -all {{delimiter}} $contents {\\t}  converted
	regsub -all {,intensity} $converted {} converted
	writeFile $propertiesFile  $converted


	if {![catch {exec sh $::boundssh -max 10 $file} err]} {
	    puts "OK: [file tail $file]"
	    return;
	}


	puts "Removing the rgb fields: $file"
	regsub -all {{delimiter}} $contents {\\t}  converted
	regsub -all {red,green,blue,intensity} $converted {} converted
	writeFile $propertiesFile  $converted
	if {[catch {exec sh $::boundssh -max 10 $file} err]} {
	    if {[regexp {bad token cnt} $err]} {
		puts "Error: not xyzrgbi - $file  "
	    } else {
		puts "Error: $file\n$err"
	    }
	} else {
	    puts "File is xyzrgb: $file"
	}
    }


}

for {set i 0} {$i<$argc} {incr i} {
    set arg [lindex $argv $i]
    walkTree $arg
}