##@(https://en.wikipedia.org/wiki/Benton_County,_Arkansas imageWidth=100 addHeader=false ignoreError=true)




set ::list {kootenai colville jamestown elwha samish ronde siletz yakama gamble warm northwestern kasaan summit {duck valley} siuslaw umatilla}

proc geojson {tribe file} {
    set text [string tolower $tribe]
    if {[file exists $file] && [file size $file]>100} {
#	puts "ok: $tribe"
	return 1
    }

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
    regsub -all { } $text {+} text
    set url "https://ramadda.org/repository/entry/show?entryid=c6969703-c500-49ad-9fe5-ee14c19a5b2d&output=geojsonfilter&geojson_property=name&geojson_filter=Subset&geojson_value=(?i).*$text.*"
    catch {exec wget -O $file $url}
    if {[file size $file]<100} {
	puts "not ok: $tribe text:$text"
	file delete $file
	return 0
    }
#    puts "ok: $tribe"
    return 1
}

puts "<entries>"
proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}


proc attr {n v} {
    return " $n=\"$v\" "
}

proc tribe {state tribe wiki lat lon} {
#    puts "$tribe $lat $lon"
    set file [string tolower $tribe]
    regsub -all { } $file _ file
    regsub -all {[\(\)]+} $file {} file    
    set id $file
    regsub -all {'} $id {} id
    set file "$file.geojson"
    puts "<entry [attr type tribal_datahub] [attr name $tribe] [attr id $id]";
    set desc "+callout-info\nThis is an example data hub for the $tribe\n-callout\n"
    append desc "@($wiki imageWidth=100 addHeader=false ignoreError=true)\n"

    set bounds ""
    set north ""
    set west ""
    set south ""
    set east ""
    if {[file exists $file]} {
	set bounds [exec java org.ramadda.util.geo.GeoJson -bounds $file]
	regexp {north:(.*) +west:(.*) +south:(.*) +east:(.*)} $bounds match north west south east
	set lat [expr $south+($north-$south)/2]
	set lon [expr $west+($east-$west)/2]
	puts "[attr north $north] [attr west $west] [attr south  $south] [attr east $east]"
	puts ">"
	puts "<metadata [attr type map_displaymap_file] [attr inherited true]>"
	puts "<attr [attr fileid $file] [attr index 1] [attr encoded false]>[cdata $file]</attr>"
	puts "</metadata>"
    } else {
	if {$lat!="N/A"} {
	    puts " [attr latitude $lat] [attr longitude $lon] "
	}
	puts ">"
    }
    puts "<state>[cdata $state]</state>\n"
    puts "<metadata [attr type content.alias]><attr [attr index 1] [attr encoded false]><!\[CDATA\[tribe_$id\]\]></attr></metadata>"
    set children ""
    append children "<entry [attr type group] [attr name Data] [attr parent $id] [attr id ${id}_data]/>\n"
    append children "<entry [attr type type_document_collection] [attr name Documents] [attr parent $id]/>\n"    
    if {$north!=""} {
	set cy [expr $south+($north-$south)/2]
	set cx [expr $west+($east-$west)/2]
	set d 1
	set bbox "[expr $cy+$d],[expr $cx-$d],[expr $cy-$d],[expr $cx+$d]"
	append children "<entry [attr type type_virtual] [attr name {Local Weather Stations}] [attr parent ${id}_data]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_awc_metar\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"

	append children "<entry [attr type type_virtual] [attr name {USGS Stream Gauges}] [attr parent ${id}_data]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_usgs_gauge\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"


	set d 2
	set bbox "[expr $cy+$d],[expr $cx-$d],[expr $cy-$d],[expr $cx+$d]"
	append children "<entry [attr type type_virtual] [attr name {NOAA Sea-Level Trends}] [attr parent ${id}_data]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_noaa_tides_trend\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"

    }

    if {$lat!="N/A"} {
	set name "$tribe Current Conditions"
	append children "<entry [attr type nwsfeed] [attr name $name] [attr id ${id}_nws] [attr parent ${id}_data] [attr latitude $lat] [attr longitude $lon]/>\n"
	set name "$tribe Historic Weather"
	append children "<entry [attr type type_daymet] [attr name $name] [attr parent ${id}_data] [attr latitude $lat] [attr longitude $lon]/>\n"	
	append desc ":heading Current Conditions\n"
	append desc "{{nws.hazards entry=\"${id}_nws\" addHeader=\"false\"}}\n"
	append desc ":br\n"
	append desc "{{nws.current [attr entry ${id}_nws] [attr addHeader false] [attr orientation vertical]}}\n";
	append desc ":br\n"
	append desc "{{nws.forecast [attr entry ${id}_nws] showHeader=false showDetails=false count=1000}}\n";

	
    }
    puts  "<description><!\[CDATA\[$desc\]\]></description>\n"
    puts "</entry>"    
    puts $children
#    geojson $tribe $file
}

source tribes.tcl
puts "</entries>"
