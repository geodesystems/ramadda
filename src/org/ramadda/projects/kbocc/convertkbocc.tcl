set ::fp [open loggers.csv w]
puts $::fp {site,latitude,longitude}

proc convert {site f latitude longitude notes} {
    if {![info exists ::loc($site,lat)]} {
	set ::loc($site,lat) "$latitude"
	set ::loc($site,lon) "$longitude"
	puts $::fp "$site,$latitude,$longitude"
    } else {
	if {!($::loc($site,lat) eq $latitude) || !($::loc($site,lon) eq $longitude)} {
	    puts stderr "bad loc: $site $latitude $longitude should be $::loc($site,lat) $::loc($site,lon)"
	}
    }
    set file files/$f
    if {![file exists $file]} {
	puts stderr "No file: $file"
	return
    }	
    if {![file exists converted]} {
	exec mkdir converted
    }
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
    file rename -force $file  $newFile
}




set lines [read [open files.csv r]]
set cnt 0
foreach line [split $lines "\n"] {
    set line [string trim $line]
    if {$line==""} continue
    incr cnt
    if {$cnt==1} continue
    foreach {site file latitude longitude notes} [split $line ,] break
    convert  $site $file $latitude $longitude $notes
}


close $::fp
