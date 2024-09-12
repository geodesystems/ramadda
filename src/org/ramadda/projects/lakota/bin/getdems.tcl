#https://www.sdgs.usd.edu/digitaldata/dem.html

proc max {n1 n2 n3 n4} {
    if {$n1>$n2 && $n1>$n3 && $n1 > $n4} {return $n1}
    if {$n2>$n3 && $n2 > $n4} {return $n2}
    if {$n3 > $n4} {return $n3}        
    return $n4
}


proc min {n1 n2 n3 n4} {
    if {$n1<$n2 && $n1<$n3 && $n1 < $n4} {return $n1}
    if {$n2<$n3 && $n2 < $n4} {return $n2}
    if {$n3 < $n4} {return $n3}        
    return $n4
}



set ::entries "<entries>"
proc fetch {base} {
    set file ${base}.zip
    if {![file exists $file]} {
	puts "fetching $file"
	catch {exec wget -O $file "http://mri.usd.edu/basedata/dem/30m/${base}.zip"}
    }

    if {[file size $file]==0} {
	puts stderr "empty $file"
	return
    }
    if {![file exists $base]} {
	catch {exec  unzip $file}
    }	
    set files [glob -nocomplain $base/*/hdr.adf] 
    set tif $base.tif
    if {![file exists $tif]} {
	puts "Making tif $tif"
	eval exec /usr/local/bin/gdal_merge.py -o $tif  $files
    }

    set thumb ${base}_thumb.png
    if {![file exists $thumb]} {
	puts "making thumbnail $thumb"
	exec gdaldem hillshade $base.tif hillshade.tif -z 10.0 -s 1.0 -az 315 -alt 45
	exec gdal_translate -of JPEG hillshade.tif  tmp.jpg
	exec convert -resize 600x tmp.jpg $thumb
    }

    set json [exec gdalinfo -json $tif]
    regexp {wgs84Extent(.*)} $json match json
    regsub -all {\n} $json { } json
    regsub -all { } $json {} json    
    set n [regexp -all -inline {[-]?\d+\.\d+} $json]
    set east [max [lindex $n 0] [lindex $n 2] [lindex $n 4] [lindex $n 6]]
    set west [min [lindex $n 0] [lindex $n 2] [lindex $n 4] [lindex $n 6]]
    set north [max [lindex $n 1] [lindex $n 3] [lindex $n 5] [lindex $n 7]]
    set south [min [lindex $n 1] [lindex $n 3] [lindex $n 5] [lindex $n 7]]    
    puts "$north $west $south $east"
    append ::entries "<entry type=\"geo_geotiff\" file=\"$tif\" filename=\"$tif\" name=\"S.D. DEM - $base\" north=\"$north\"  south=\"$south\"  east=\"$east\" west=\"$west\" >\n"
    append ::entries {<metadata type="content.thumbnail">}
    append ::entries "<attr encoded=\"false\" fileid=\"$thumb\" index=\"1\"><!\[CDATA\[$thumb\]\]></attr>"
    append ::entries {<attr encoded="false" index="2"><![CDATA[]]></attr><attr encoded="false" index="3"><![CDATA[false]]></attr></metadata>}
    append ::entries {</entry>\n}
}

set cnt 0
foreach lat {42 43 44 45} {
    foreach lon {096 097 098 099 100 101 102 103 104} {
	set base "${lat}${lon}"
	fetch $base
	incr cnt
    }
}

append ::entries "</entries>\n"
puts [open entries.xml w] $::entries
