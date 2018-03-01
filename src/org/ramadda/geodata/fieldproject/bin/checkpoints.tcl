source [file join [file dirname [info script]] lib.tcl]

set ::fields {",red,green,blue,intensity" ",red,green,blue" ",intensity" "" }
set ::boundssh $::pointtoolsdir/pointbounds.sh

proc run {file} {
    return [exec sh $::boundssh -max 10 $file]
}

proc isFileOk {file} {
    if {[catch {run $file} err]} {
	return 0;
    }
    if {![regexp {\s+(-?[\d\.]+)\s+(-?[\d\.]+)\s+(-?[\d\.]+)\s+(-?[\d\.]+)\s*} $err match north west  south east]} {
	puts "no output: $err"
	return 0
    }

    set height [expr abs($north-$south)]
    set width [expr abs($east-$west)]

    if {$height>5 || $width>5} {
	puts "Error: maybe wrong coordinate system for file: $file"
	puts "bounds are north:$north west:$west south:$south east:$east"
	exit
    }

    puts "OK: [file tail $file] lat: [format {%.5f} $north] - [format {%.5f} $south] lon: [format {%.5f} $east] - [format {%.5f} $west]"
    return 1
}



#Recurse down the tree
proc walkTree {file {crs {}} {utmZone {}} {utmNorth {}}} {
    set tail [file tail $file]

    if {[file isdirectory $file]} {
	if {[regexp {.*ECEF.*} $tail]} {
	    set crs ecef
	} elseif {[regexp {.*Lat.*Lon} $tail]} {
	    set crs latlon
	} elseif {[regexp {.*UTM-?([0-9]+)(S|N)} $tail match zone hemisphere]} {
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
	set contents ""
	switch $crs {
	    ecef {
		set contents [read [open $::resourcedir/ecef_point.properties r]]
	    }
	    latlon {
		set contents [read [open $::resourcedir/latlon_point.properties r]]
	    }
	    utm {
		set contents [read [open $::resourcedir/utm_point.properties r]]
		regsub -all {{utm.zone}} $contents $utmZone contents
		regsub -all {{utm.north}} $contents $utmNorth contents
	    }
	}
	
	if {$contents==""} {
	    puts "Error: Could not determine CRS for: $file"
	    return
	}

	set commonFile [file join $parent point.properties]
	set specificFile "${file}.properties"

	if {[file exists $specificFile]} {
	    set  propertiesFile $specificFile
	} else {
	    set  propertiesFile $commonFile
	}
	set havePropertiesFile [file exists $propertiesFile]

#	puts "file:  [file tail $file] -- props: $havePropertiesFile"
	if {$havePropertiesFile} {
	    if {[isFileOk $file]} {
		return;
	    }
	    ##TODO: use the filename as the properties file
	    set propertiesFile "${file}.properties"
	    puts "File could not be read with default point.properties file: [file tail $file]\n\tWriting custom [file tail $propertiesFile]"
#	    return;
	}

	set tries 0
	foreach delimiter {"," "" "tab"} {
	    foreach field $::fields {
		if {$tries>1} {
		    puts "trying delimiter: $delimiter  fields: $field"
		}
		set tmp $contents
		regsub -all {{delimiter}} $tmp $delimiter  tmp
		regsub -all {{fields}} $tmp $field tmp
		writeFile $propertiesFile  $tmp
		if {[isFileOk $file]} {
		    return;
		} else {
#		    puts "ERROR: $err"
#		    puts "FILE: $file"
		}
		incr tries
	    }
	}
	puts "Error: could not figure out delimiter or fields for file: $file"
	exit
    }


}

for {set i 0} {$i<$argc} {incr i} {
    set arg [lindex $argv $i]
    if {$arg == "-fields"} {
	incr i
	set ::fields [lindex $argv $i]
	continue
    }
    walkTree $arg
}