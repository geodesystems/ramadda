source ../lib.tcl
set ::entries "<entries>\n";

set ::cnt 0
set ::bp {class_1 division kingdom class family order record_status taxon sci_name common_name catalog handling object_status quantity description status_date tsn condition_desc prep_material country county state lakota_names cultural_uses}
proc bio $::bp  {
    incr ::cnt
#    puts stderr "#$::cnt"
##    if {$::cnt>7} return

    foreach p $::bp {
	set v [string trim [clean [set $p]]]
	set v [spell $v]
	set $p $v
	check "Bio Records" $::cnt $p $v
    }
    if {[regexp -nocase {Does not require} $handling] || [regexp -nocase {Doesn't require special handling} $handling]} {
	set handling {Does not require special handling}
    }
    set seenLakota 0
    set desc ""
    set lakota {}
    foreach line [split $description "\n"] {
	set line [string trim $line]
	if {[regexp -nocase {^Lakota name:} $line]} {
	    set seenLakota 1
	    regsub -nocase {^Lakota name:} $line {} line
	    set line [string trim $line]	    
	}
	if {$line=="-" || $line==""} {
	    set seenLakota 0
	    continue;
	}

	if {$line==""} continue
	if {$seenLakota} {
	    if {[regexp {^-} $line]} {
		set seenLakota 0
 	    } else {
		lappend lakota [string  trim "$line"]
		continue
	    }
	}
	append desc "$line\n"
    }

    set desc [string trim $desc]	    
    set name ""
    set cnames {}
    array set seen {}
    regsub -all -- {--} $common_name "\x1F" common_name
    foreach n [split $common_name "\x1F"] {
	set n [string trim $n];
	if {[info exists seen($n)]} continue;
	set seen($n) 1
	if {$n==""} continue;
	if {$name==""} {
	    set name $n
	    continue
	}
	lappend cnames $n
    }
    set common_name [join $cnames "\n"]
    if {$name==""} {set name $sci_name}
    append ::entries [openEntry type_archive_bio {} {} $name]    
    set rand [expr {rand()}]
    append ::entries [col latitude [expr {40+($rand*20)}]]
    set rand [expr {rand()}]
    append ::entries [col longitude [expr {-130+($rand*20)}]]    


    if {$status_date!=""} {
	append ::entries [col fromdate $status_date]
	append ::entries [col todate $status_date]	    
    }
    set desc [fixme $desc]




    append ::entries [col description $desc]    
    append ::entries [col catalog_number $catalog]    
    append ::entries [col common_name $common_name]


    set tsn2 ""
    regsub -all  {\|\|} $taxon "\x1F" taxon
    foreach n [split $taxon "\x1F"] {
	regsub -all {  +} $n { } n
	set toks [split $n { }]
	foreach {taxa v tsn2}  $toks break
	set taxa [string tolower $taxa]
	regsub -all {__} $v {} v
	regsub -all {__} $tsn2 {} tsn2	
	set taxa [string trim $taxa]
	set v [string trim $v]
	set tsn2 [string trim $tsn2]
	append ::entries [col taxon_${taxa} $v]
#	puts "TAX:$taxa"
    }


    if {0} {
	append ::entries [col taxon_kingdom $kingdom]
	append ::entries [col taxon_division $division]
	append ::entries [col taxon_class $class]    
	append ::entries [col taxon_order $order]    
	append ::entries [col taxon_family $family]
    }

    append ::entries [col tsn_number $tsn]
    regsub -all {  +} $sci_name { } sci_name
    append ::entries [col scientific_name $sci_name]    

    if {$object_status=="STORAGE"} {set object_status Storage}
    append ::entries [col object_status $object_status]
    append ::entries [col handling $handling]
    append ::entries [col condition $condition_desc]    
    append ::entries [col quantity $quantity]
    append ::entries [col preparation_material $prep_material]            

    
    append ::entries [col country US]
    append ::entries [col state {South Dakota}]
    append ::entries [col county $county]

    if {$cultural_uses!=""} {
	append ::entries [mtd1 archive_cultural_use $cultural_uses]
    }

    array  set seenalt {}
    if {[llength $lakota]} {
	foreach l $lakota {
	    foreach l2 [split $l ,] {
		set l2 [string trim $l2]
		if {$l2==""} continue
		foreach l3 [split $l2 ;] {
		    set l3 [string trim $l3]
		    if {$l3==""} continue
		    if {[info exists seenalt($l3)]} {continue}
		    set seenalt($l3) 1
		    append ::entries [mtd2 archive_alternate_name {Lakota Name} [fixme $l3 1 $name]]
		}
	    }
	}
    }

    if {$lakota_names!=""} {
	append ::entries [mtd2 archive_alternate_name {Lakota Name} $lakota_names]
    }    

    append ::entries "</entry>\n"
}


source bio.tcl
append ::entries "</entries>\n";

set fp [open bioentries.xml w]
puts $fp $::entries
close $fp


