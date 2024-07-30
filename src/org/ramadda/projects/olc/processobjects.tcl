source ../lib.tcl
set ::entries "<entries>\n";
set ::cnt 0 
array set ::cids {}

set ::params {catalog_number accession_number object_name category object_class object_subclass object_subclass2 object_subclass3 object_subclass4 full_classif title artist_maker date_made description material location lakota_name cultural_uses collection_name collection_number}



proc object $::params  {
    incr ::cnt
#    if {$::cnt>2} return
    foreach p $::params {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check object $::cnt $p $v]
    }

    set material [string totitle $material]
    set category [string totitle $category]    
    
    set description [clean $description]
    set parent [cid $collection_number]
    if {![info exists ::cids($parent)]} {
	append ::entries [openEntry type_archive_collection $parent "" $collection_name]
	append ::entries "</entry>\n"
	set ::cids($parent) 1
    }
    append ::entries [openEntry type_archive_object {} $parent $title]    
    set year $date_made
    regsub -all s $year {} year
    regsub -all {[^0-9]} $year {} year    
    set year [string trim $year]
    if {![regexp {^\d\d\d\d$} $year]} {
	set year ""
    }
    if {$year !=""} {
	append ::entries [col fromdate $year]    
    }

    append ::entries [col description $description]    

    foreach p {catalog_number accession_number object_name artist_maker category object_class object_subclass object_subclass2 object_subclass3 object_subclass4 } {
	append ::entries [col $p [set $p]]
    }

    set l $location
    regsub -all __ $location > location
    regsub -all -nocase {none} $location {} location    
    regsub -all {\s+>} $location {>} location
    regsub -all {>>+} $location {>} location    
    regsub -all {case} $location {Case} location
    regsub -all {shelf} $location {Shelf} location
    regsub -all {collection} $location {Collection} location        
    append ::entries [col location  $location 0]
    append ::entries [mtd1 archive_object_material $material]

    append ::entries "</entry>\n"
}


catch {exec sh /Users/jeffmc/bin/seesv.sh -template {} {object {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}} {${15}} {${16}} {${17}} {${18}} {${19}} }  {\n} {} objects.csv > objects.tcl}


source objects.tcl
append ::entries "</entries>\n";

set fp [open objectsentries.xml w]
puts $fp $::entries
close $fp






