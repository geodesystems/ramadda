
source $env(RAMADDA_ROOT)/bin/ramadda.tcl

set ::entryOrder 0

proc fetch_url {url filepath} {
    # Run wget and capture output and errors
    set status [catch {exec wget --server-response --quiet --output-document=$filepath $url 2>@1} output]
    set ::fetch_url_message ""
    # Check for 404 error in output
    if {[string match "*404 Not Found*" $output]} {
	set ::fetch_url_message "Error: 404 Not Found - $url"
	# Remove the incomplete file if created
        file delete -force $filepath  ;
	return 0
    } elseif {$status == 0} {
#        puts stderr "Download successful: $filepath"
	return 1
    } else {
	set ::fetch_url_message  "Download failed: $output"
	return 0
    }

}




proc img {region rname t tname p pname} {
    set file ${t}${p}${region}.png
    set bad "images/bad$file"
    if {[file exists $bad]} {
	return
    }

    set _region [string tolower $region]
    if {[regexp  {hprcc-.*} $_region]} {
	regsub -all hprcc- $_region hprcc/ _region
    }
    set url "https://hprcc.unl.edu/products/maps/acis/$_region/$file"


    if {$_region=="us"} {
	set url "https://hprcc.unl.edu/products/maps/acis/$file"
    }


    if {![file exists "images/$file"] || [file size images/$file]==0} {
	exec mv images/$file images/bad$file
	return
    }


    if {![file exists "images/$file"]} {
	if {![fetch_url $url images/$file]} {
	    exec touch $bad
	    puts stderr "Error:  Region: $region Time: $t Product: $p File: $file"
	    puts stderr "\t$::fetch_url_message"
	    return
	}
	puts stderr "OK: $region Time: $t Product: $p File: $file"
    }

    set parent [getGroupId $region $tname]


    append ::xml [openEntry type_image_product "" "$parent" "$rname - $pname - $tname"  url $url entryorder [incr ::entryOrder]]

    append ::xml [col product $pname]
    append ::xml [col date_specifier $tname]    
    append ::xml [col region $region]
    append ::xml [mtd2 metadata_source {https://hprcc.unl.edu/maps.php?map=ACISClimateMaps} {ACIS Climate Maps}]
    append ::xml [closeEntry]
}



proc getGroupId {region tname} {
    return "$region - $tname"
}

proc images {region rname t tname} {
    set id [getGroupId $region $tname]
    append ::xml   [openEntry group $id $region "ACIS Climate Maps - $rname - $tname" entryorder [incr ::entryOrder]]
    append ::xml [closeEntry]
    img $region $rname $t  $tname PData {Precipitation}
    img $region $rname $t  $tname PDept {Departure from Normal Precipitation}
    img $region $rname $t  $tname PNorm {Percent of Normal Precipitation}
    img $region $rname $t  $tname SPIData {Standardized Precipitation Index (SPI)}
    img $region $rname $t  $tname SPEIData {Standardized Precipitation Evapotranspiration Index (SPEI)}
    img $region $rname $t  $tname TData {Temperature}
    img $region $rname $t  $tname TDept {Departure from Normal Temperature}
    img $region $rname $t  $tname TMAXData {Average Maximum Temperature}
    img $region $rname $t  $tname TMINData {Average Minimum Temperature}
    img $region $rname $t  $tname TMAXDept {Departure from Normal Average Maximum Temperature}
    img $region $rname $t  $tname TMINDept {Departure from Normal Average Minimum Temperature}
    img $region $rname $t  $tname TMAXMAX {Maximum Temperature (Highest 1-day)}
    img $region $rname $t  $tname TMINMIN {Minimum Temperature (Lowest 1-day)}
    img $region $rname $t  $tname TMAXDept {Departure from Normal Maximum Temperature}
    img $region $rname $t  $tname TMINDept {Departure from Normal Minimum Temperature}
    img $region $rname $t  $tname C65Data {Cooling Degree Days (Base 65)}
    img $region $rname $t  $tname C65Dept {Departure from Normal CDD (Base 65)}
    img $region $rname $t  $tname H65Data {Heating Degree Days (Base 65)}
    img $region $rname $t  $tname H65Dept {Departure from Normal HDD (Base 65)}
}

set ::xml "<entries>\n"

#foreach region {US HPRCC {HPRCC-SD}} {}
foreach region {US HPRCC  {{HPRCC-SD} {South Dakota}}} {
    set rname $region
    if {[llength $rname]==2} {
	foreach {region rname} $rname break
    }

    puts $rname
    append ::xml   [openEntry group $region "" "ACIS Climate Maps - $rname" entryorder [incr ::entryOrder]]
    append ::xml [col description {<wiki>\n+section title={{name}}\n{{frames  width=100%  height=800px  showIcon=false}}\n-section\n}]
    append ::xml [closeEntry]

    images $region $rname 7d {Last 7 Days}
    images $region $rname  14d {Last 14 Days}
    images $region $rname  30d {Last 30 Days}
    images $region $rname  60d {Last 60 Days}
    images $region $rname  90d {Last 90 Days}
    images $region $rname  120d {Last 120 Days}
    foreach m {6 9 12 24 36 48 60} {
	images $region $rname  "${m}m" "${m}-Months"
    }
    images $region $rname  WaterP "Since Oct 1 (Water Year)"
    images $region $rname Last1m "Last Full Month"
    images $region $rname Last3m "Last 3 Months"
    images $region $rname  Last12m "Last 12 Months"
}


append ::xml "</entries>"
set fp [open entries.xml w]
puts $fp $::xml
close $fp
puts "$::entryOrder"
