set ::sitesfp [open sites.xml w]
puts $::sitesfp "<entries>"

if {![file exists converted]} {
    exec mkdir converted
}



set ::currentSite ""
proc site {site lat lon} {
    set ::currentSite $site
    if {![info exists ::loc($site,lat)]} {
	set ::loc($site,lat) "$lat"
	set ::loc($site,lon) "$lon"
	puts $::sitesfp "<entry name=\"$site\" type=\"type_kbocc_site\" latitude=\"$lat\" longitude=\"$lon\"/>"
    } else {
	puts stderr "dup site: $site"
    }
}



proc convert {f} {
    set file /Users/jeffmc/kbocc/rawfiles/$f
    if {![file exists $file]} {
	puts stderr "No file: $file"
	return
    }	
    set site $::currentSite
    set yy ""
    set inst ""
    foreach pattern {{(\d\d)-(\d\d)} {(\d\d)_(\d\d)} {(\d\d)-(\d)} {^(\d\d)} {(\d\d\d\d)}} {
	if {[regexp  $pattern $f match yy inst]} {
	    break
	}
    }
    if {$yy==""} {
	puts stderr "no year $f"
	return
    }

    if {[string length $yy]==2} {
	set yy "20${yy}"
    }
    regsub -all { +} $site _ site
    if {$inst !=""} {
	set inst "_$inst"
    }
    set newFile [file join converted "${site}_$yy$inst.csv"]
    file copy -force $file  $newFile

    #Now move it to rawfiles2 for now
    set newFile /Users/jeffmc/kbocc/rawfiles2/$f
    file rename -force $file  $newFile
}


source files.tcl

puts $::sitesfp "</entries>"
close $::sitesfp

foreach file [glob -nocomplain  /Users/jeffmc/kbocc/rawfiles/*] {
    puts "No match for file $file"
}


foreach file [glob -nocomplain  /Users/jeffmc/kbocc/rawfiles2/*] {
    file rename $file [file join /Users/jeffmc/kbocc/rawfiles [file tail $file]]
}


set fileList [glob -nocomplain converted/*]
set command [list zip converted.zip]
foreach file $fileList {
    lappend command $file
}

eval exec $command
