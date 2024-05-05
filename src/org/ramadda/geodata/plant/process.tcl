set ::sitefp [open sites.csv w]
puts $::sitefp "site,latitude,longitude"
set ::entriesfp [open entries.xml w]
puts $::entriesfp "<entries>"
set ::shfp [open create.sh w]
puts -nonewline $::shfp "zip  /Users/jeffmc/plant.zip entries.xml "

set ::cnt 0

proc col {list index} {
    set v [string trim [lindex $list $index]]
    regsub -all --  _comma_ $v , v
    set v
}

proc process {site type file} {

    set datafile "${site}_${file}.csv"
    set file_size [file size $datafile]
    if {$file_size>4000000} {
#	puts "big $datafile $file_size"
	return;
    }
    incr ::cnt
#    if {$::cnt >10} return
    puts stderr "processing site:$site #$::cnt"
    set infofile "${site}_info.csv"
    if {![file exists $infofile]} {
	exec sh /Users/jeffmc/bin/seesv.sh -c si_name,si_country,si_biome,si_contact_institution,si_remarks,si_lat,si_long -change 0-10 , _comma_ -noheader -p "${site}_site_md.csv" > $infofile
    }
    set contents [string trim [read [open $infofile r]]]
    set loc [split $contents ,]
    set idx -1
    regsub -all {_} $site  {-} _site
    set name [col $loc [incr idx]]
    regsub -all {_} $name { }  name
    set name "$name - $_site"
    set country [col $loc [incr idx]]
    set biome [col  $loc [incr idx]]
    set institution [col  $loc [incr idx]]
    regsub -all {\n} $institution { } institution 
    set remarks [col $loc [incr idx]]
    if {$remarks=="NA"} {set remarks ""}
    set lat [col $loc [incr idx]]
    set lon [col $loc [incr idx]]
    puts $::sitefp "$site,$lat,$lon"
    flush $::sitefp
    puts -nonewline $::shfp " $datafile "
    puts $::entriesfp "<entry  name=\"$name\" site_id=\"$site\" country=\"$country\" biome=\"$biome\"  type=\"$type\" latitude=\"$lat\" longitude=\"$lon\" file=\"$datafile\">"
    puts $::entriesfp "<institution><!\[CDATA\[$institution\]\]></institution>"
    puts $::entriesfp "<description><!\[CDATA\[$remarks\]\]></description>"
    puts $::entriesfp "</entry>"
    flush $::entriesfp
}



set script_dir [file dirname [info script]]

source "$script_dir/plantsites.tcl"
set entrytype type_sapflux_plant_sapf
set ::typefp [open /Users/jeffmc/planttypes.xml w]
puts $::typefp "<types supercategory=\"Geoscience\" category=\"Plant Data\">"
puts $::typefp "<type name=\"$entrytype\" super=\"point_text_csv\" description=\"Plantflux Test\" handler=\"org.ramadda.data.services.PointTypeHandler\">"
puts $::typefp "<property name=\"icon\" value=\"/plant/tree.png\"/>"
puts $::typefp "<property name=\"form.area.show\" value=\"false\"/>"
puts $::typefp "<property name=\"record.file.class\" value=\"org.ramadda.data.point.text.CsvFile\"/>"
puts $::typefp {<column  name="site_id"  type="string" label="Site ID" cansearch="true"/>}
puts $::typefp {<column  name="country"  type="enumeration" label="Country" cansearch="true"/>}
puts $::typefp {<column  name="biome"  type="enumeration" label="Biome" cansearch="true"/>}
puts $::typefp {<column  name="institution"  type="enumeration" label="Institution" cansearch="true"/>}
puts $::typefp {
<property name="record.properties"> 
csvcommands1=-addheader, timestamp.type date   timestamp.format {yyyy-MM-dd'T'HH:mm:ss'Z'}
</property>
}

puts $::typefp {
<property name="bubble">
<![CDATA[ 
+section title={{name}}
{{display_linechart fields=#3}}
{{display_linechart fields=#4}}
-section
]]></property>
}
puts $::typefp {
<embedwiki>
<![CDATA[ 
+section title={{name}}
{{display_linechart fields=#3}}
{{display_linechart fields=#4}}
-section
]]></embedwiki>
}

puts $::typefp {
<wiki><![CDATA[
+section title={{name}}
+row
+col-8
{{display_linechart fields=#3}} 
{{display_linechart fields=#4}} 
{{display_linechart fields=#5}} 
-col
+col-4
{{mapentry}}
-col
-row
{{display_download}}
{{display_table}}
:heading Information
{{information details=true}}
-section
]]></wiki>
}


puts $::typefp "</type>"
puts $::typefp "</types>"
close $::typefp


foreach site $sites {process $site $entrytype sapf_data}

close $::sitefp
puts $::entriesfp "</entries>"
flush $::entriesfp
close $::entriesfp
puts  $::shfp ""
close $::shfp
