package require csv
package require http




set ::entriesfp [open entries.xml w]
proc write {s} {
    puts $::entriesfp $s
}


proc index {base key} {
    set data "::${base}key"
    for {set i 0} {$i<[llength [set $data]]} {incr i} {
	if {[lindex [set $data] $i] == $key} {
	    return $i
	}
    }
    return -1
}
proc fetch {base line key} {
    set idx [index $base $key]
    lindex $line $idx
}

proc find {base key name} {
    foreach line [set "::$base"] {
	set tribe [fetch $base $line $key]
	if {[regexp -nocase $name $tribe]} {
	    return $line
	}
    }
#    puts stderr "NO MATCH: $name"
    return {}
}

proc parseCsvFile {base filename} {
    set fp [open $filename r]
    set csvData [read $fp]
    close $fp

    set "::$base" {}
    set cnt 0
    foreach row [split [string trim $csvData] \n] {
	set line  [csv::split $row]
	if {$cnt==0} {
	    set idx 0
	    foreach key $line {
		set key [string tolower $key]
		incr idx
	    }
	    set "::${base}key" $line
	} else {
	    lappend "::$base" $line
	}
	incr cnt
    }
    set "::$base"
}



set fp [open template.xml r]
set ::template [read $fp]
close $fp
set fp [open toptemplate.xml r]
set ::toptemplate [read $fp]
close $fp


# Main script execution
parseCsvFile bia BIA_Tribal_Leadership_Directory.csv
parseCsvFile terroritories territories.csv

set ::list {kootenai colville jamestown elwha samish ronde siletz yakama gamble warm northwestern kasaan summit {duck valley} siuslaw umatilla}


proc cleanTribe {tribe} {
    set text [string tolower $tribe]
    foreach v $::list {
	if {[regexp $v  $text]} {
	    set text $v
#	    puts "XXX $v $tribe"
	    break
	}
    }

    regsub -all -nocase tribes $text { } text
    regsub -all -nocase tribe $text { } text
    regsub -all -nocase nation $text { } text    
    set text [string trim [string tolower $text]]
}     

proc geojson {tribe file} {
    if {[file exists $file] && [file size $file]>100} {
#	puts "ok: $tribe"
	return 1
    }

    set text [cleanTribe $tribe]
    regsub -all { } $text {+} text
    set url "https://ramadda.org/repository/entry/show?entryid=c6969703-c500-49ad-9fe5-ee14c19a5b2d&output=geojsonfilter&geojson_property=name&geojson_filter=Subset&geojson_value=(?i).*$text.*"
    catch {exec wget -O $file $url}
    if {[file size $file]<100} {
	puts stderr "not ok: $tribe text:$text"
	file delete $file
	return 0
    }
#    puts "ok: $tribe"
    return 1
}


write "<entries>"
proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}


proc attr {n v} {
    return " $n=\"$v\" "
}

proc findSlug {tribe} {
    if {[regexp -nocase {Cow Creek Band} $tribe]} {
	return cow-creek-umpqua
    }
    if {[regexp -nocase {sauk} $tribe]} {
	return sauk-suiattle
    }
    if {[regexp -nocase {burns paiute} $tribe]} {
	return northern-paiute
    }
    if {[regexp -nocase {duck valley} $tribe]} {
	return northern-paiute
    }
    if {[regexp -nocase {summit lake} $tribe]} {
	return northern-paiute
    }            
    if {[regexp -nocase {Northwestern Band of Shoshone} $tribe]} {
	return newe-western-shoshone
    }        

    if {[regexp -nocase {Chippewa Cree} $tribe]} {
	return cree
    }        
    


    if {[regexp -nocase klallam $tribe]} { set tribe klallam}        
    set key [cleanTribe $tribe]
    regsub -all upper $key {} key


    if {[regexp tolowa $key]} { set key tolowa}
    if {[regexp squaxin $key]} { set key squaxin}
    if {[regexp haida $key]} { set key haida}
    if {[regexp blackfeet $key]} { set key blackfoot}
    set key [string trim $key]
    if {[string length $key]<=3} {
	puts stderr "NO $tribe key: $key"
	return ""
    }
    set cnt 0
    foreach line $::terroritories {
	set name [lindex  $line 2]
	if {[regexp -nocase $key $name]} {
	    set slug [lindex  $line 0]
	    return $slug
	}
    }
    puts stderr "NO $tribe key: $key"

    return ""
}

set ::tribecnt 0
proc tribe {state tribe wiki lat lon} {
    incr ::tribecnt
    if {$::tribecnt>2} return
    set key [cleanTribe $tribe]
    set biaInfo [find  bia  tribe $key]
    set file [string tolower $tribe]
    regsub -all { } $file _ file
    regsub -all {[\(\)]+} $file {} file    
    set id $file
    regsub -all {'} $id {} id
    set file "$file.geojson"
    write "<entry [attr type tribal_datahub] [attr name $tribe] [attr id $id]";
    set desc "+callout-info\nThis is an example data hub for the $tribe\n-callout\n"
    append desc "@($wiki imageWidth=100 addHeader=false ignoreError=true)\n"

    set metadata ""
    set attrs ""    
    if {[llength $biaInfo]>0} {
	set lat [fetch bia $biaInfo Y]
	set lon [fetch bia $biaInfo X]	
	set url [string trim [fetch bia $biaInfo website]]
	if {$url!=""} {
	    append metadata "<metadata [attr type content.url]>"
	    append metadata "<attr  [attr index 1] [attr encoded false]>[cdata $url]</attr>"
	    append metadata "<attr  [attr index 2] [attr encoded false]>$tribe Website</attr>"	    
	    append metadata "</metadata>"
	}
	append metadata "<metadata [attr type content.address]>"
	set idx 0
	append metadata "<attr  [attr index [incr idx]] [attr encoded false]>[cdata [fetch bia $biaInfo mailingaddress]]</attr>"
	append metadata "<attr  [attr index [incr idx]] [attr encoded false]>[cdata [fetch bia $biaInfo mailingaddresscity]]</attr>"
	append metadata "<attr  [attr index [incr idx]] [attr encoded false]>[cdata [fetch bia $biaInfo mailingaddressstate]]</attr>"
	append metadata "<attr  [attr index [incr idx]] [attr encoded false]>[cdata [fetch bia $biaInfo mailingaddresszipcode]]</attr>"
	append metadata "<attr  [attr index [incr idx]] [attr encoded false]>[cdata {United States}]</attr>"		
	append metadata "</metadata>"
    }


    set bounds ""
    set north ""
    set west ""
    set south ""
    set east ""
#    geojson $tribe $file
    if {[file exists $file]} {
	set bounds [exec java org.ramadda.util.geo.GeoJson -bounds $file]
	regexp {north:(.*) +west:(.*) +south:(.*) +east:(.*)} $bounds match north west south east
	if {$lat=="N/A"} {
	    set lat [expr $south+($north-$south)/2]
	    set lon [expr $west+($east-$west)/2]
	}
	append metadata "<metadata [attr type map_displaymap_file] [attr inherited true]>\n"
	append metadata "<attr [attr fileid $file] [attr index 1] [attr encoded false]>[cdata $file]</attr>\n"
	append metadata "<attr  [attr index 4] [attr encoded false]>[cdata black]</attr>\n"	
	append metadata "</metadata>\n"
	append attrs "[attr north $north] [attr west $west] [attr south  $south] [attr east $east]"
    } else {
	if {$lat!="N/A"} {
	    append attrs " [attr latitude $lat] [attr longitude $lon] "
	}
    }

    write $attrs
    write ">"

    write $metadata
    write "<state>[cdata $state]</state>\n"
    write "<metadata [attr type content.alias]><attr [attr index 1] [attr encoded false]><!\[CDATA\[tribe_$id\]\]></attr></metadata>"
    set children ""
    set template $::toptemplate
    regsub -all {%parent%} $template $id template
    append children $template
    append children "\n"


    set template $::template
    set dataid ${id}_data
    append children "<entry [attr type group] [attr name Data] [attr parent $id] [attr id ${dataid}]/>\n"
    regsub -all {%tribe%} $template $tribe template
    regsub -all {%latitude%} $template $lat template
    regsub -all {%longitude%} $template $lon template
    regsub -all {%parent%} $template $dataid template            
    append children $template
    append children "<entry [attr type type_document_collection] [attr name Documents] [attr parent $id]/>\n"    
    set slug [findSlug $tribe]
    if {$slug==""} {
#	puts stderr "tribe: $tribe"
    }

    if {$slug!=""} {
	set tmap "territories_${id}.geojson"
	if {![file exists $tmap]} {
	    set url  "https://native-land.ca/api/index.php?maps=territories&name=$slug"
	    puts stderr "fetching $tmap"
	    catch {exec wget -O dummy.txt $url}
	    set fp [open dummy.txt r]
	    set contents [read $fp]
	    close $fp
	    set fp [open $tmap w]
	    puts $fp "{\"type\":\"FeatureCollection\",\"features\":$contents }"
	    close $fp
	}
	set bounds [exec java org.ramadda.util.geo.GeoJson -bounds $tmap]
	regexp {north:(.*) +west:(.*) +south:(.*) +east:(.*)} $bounds match north west south east
	set mapsid ${id}_maps
	append children "<entry [attr type type_map_folder] [attr name Maps] [attr parent $id] [attr id ${mapsid}]/>\n"
	append children "<entry [attr north $north] [attr west $west] [attr south $south] [attr east $east] [attr type geo_geojson] [attr name {Traditional Territories}] [attr filename $tmap] [attr file $tmap] [attr parent $mapsid]>\n"	
	set mapdesc "+callout-info\nTradiditional territories of the $tribe. Map courtesy of \[https://native-land.ca/ Native Land Digital\]\n-callout\n"
	append children "<description>[cdata $mapdesc]</description>\n"
	append children "</entry>\n"
    }


    if {$lat !="N/A"} {
	set cy $lat
	set cx $lon
	set d 1
	set bbox "[expr $cy+$d],[expr $cx-$d],[expr $cy-$d],[expr $cx+$d]"
	append children "<entry [attr type type_virtual] [attr name {Local Weather Stations}] [attr parent ${dataid}]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_awc_metar\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"
	
	append children "<entry [attr type type_virtual] [attr name {USGS Stream Gauges}] [attr parent ${dataid}]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_usgs_gauge\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"
	set d 2
	set bbox "[expr $cy+$d],[expr $cx-$d],[expr $cy-$d],[expr $cx+$d]"
	append children "<entry [attr type type_virtual] [attr name {NOAA Sea-Level Trends}] [attr parent ${dataid}]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_noaa_tides_trend\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"
	set name "$tribe Current Conditions"
	append children "<entry [attr type nwsfeed] [attr name $name] [attr id ${id}_nws] [attr parent ${dataid}] [attr latitude $lat] [attr longitude $lon]/>\n"
	set name "$tribe Historic Weather"
	append children "<entry [attr type type_daymet] [attr name $name] [attr parent ${dataid}] [attr latitude $lat] [attr longitude $lon]/>\n"	
	append desc ":heading Current Conditions\n"
	append desc "{{nws.hazards entry=\"${id}_nws\" addHeader=\"false\"}}\n"
	append desc ":br\n"
	append desc "{{nws.current [attr entry ${id}_nws] [attr addHeader false] [attr orientation vertical]}}\n";
	append desc ":br\n"
	append desc "{{nws.forecast [attr entry ${id}_nws] showHeader=false showDetails=false count=1000}}\n";
    }
    write  "<description><!\[CDATA\[$desc\]\]></description>\n"
    write "</entry>"    
    write $children
}

source tribes.tcl
write "</entries>"

close $::entriesfp 

set command [concat exec jar -cvf tribes.zip entries.xml [glob *.geojson]]
eval $command
puts stderr "tribes.zip generated"
