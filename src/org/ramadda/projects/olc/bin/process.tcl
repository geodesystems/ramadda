
set ::entries "<entries>\n";
array set ::cids {}
array set ::sids {}
array set ::fids {}

set ::ccnt 0
set ::scnt 0
set ::fcnt 0
set ::icnt 0
array set ::cmap {}

proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}

proc col {name s} {
    if {$s==""} {
	return ""
    }
    return "<$name>[cdata [clean $s]]</$name>\n"
}    


proc pad {n} {
    if {[string length $n]==1} {
	set n "00$n"
    }   elseif {[string length $n]==2} {
	set n "0$n"
    }    
    set n
}    

proc clean {s} {
    regsub -all {\[CR\]} $s "\n" s
    regsub -all {â€œ} $s { } s
    regsub -all {â€™} $s { } s
    regsub -all {â€“} $s {-} s    
    regsub -all {â€} $s { } s
    regsub -all {__+} $s { } s
    regsub -all {dialet} $s {dialect} s
#    regsub -all {\[} $s {\\[} s
#    regsub -all {\]} $s {\\]} s    	
    set s [string trim $s]
    set s
}



proc attrs {args} {
    set e ""
    foreach {key value} $args {
	append e [col $key  $value]
    }
    set e
}
proc attr {key value} {
    return " $key=\"[clean $value]\" "
}

proc openEntry {type id parent name} {
    set e   "<entry [attr id $id] [attr type $type] ";
    if {$parent !=""} {append e [attr  parent $parent];}
    append e ">\n";
    append e [attrs name $name]
    set e
}

proc  mtd1 {type value1} {
    if {$value1==""} {return}
    set xml  "<metadata [attr type $type]>\n"
    append  xml "<attr [attr index 1] [attr encoded false]>[cdata [clean $value1]]</attr>"
    append xml "</metadata>\n"
    set xml
}


proc  mtd2 {type value1 value2} {
    if {$value1=="" || $value2==""} {return}
    set xml  "<metadata [attr type $type]>\n"
    append  xml "<attr [attr index 1] [attr encoded false]>[cdata [clean $value1]]</attr>"
    if {$value2!=""} {
	append  xml "<attr [attr index 2] [attr encoded false]>[cdata [clean $value2]]</attr>"
    }
    append xml "</metadata>\n"
    set xml
}


proc  cid {cid} {
    return collection_$cid
}

proc  sid {cid sid} {
    return "[cid $cid]_series_$sid"
}


proc  fid {cid sid fid} {
    return "[sid $cid $sid]_file_$fid"
}

proc  iid {cid sid fid iid} {
    return "[fid $cid $sid $fid]_item_$iid"
}

set ::cp {collection_title collection_nbr location organiz_arrange scope_content bio_org_history user_1 notes provenance column1 _1 _2 _3}
proc collection $::cp  {
    incr ::ccnt
    foreach p $::cp {
	set $p [string trim [set $p]]
    }

    set cid [cid $collection_nbr]
    set ::cmap($cid) $collection_title
    set ::cids($cid) 1
    append ::entries [openEntry type_archive_collection $cid "" $collection_title]
    append ::entries [col collection_number [pad $collection_nbr]]
    append ::entries [col location $location]
    append ::entries [mtd2 archive_note Note $column1]
    append ::entries [mtd2 archive_note Arrangement $organiz_arrange]
    append ::entries [mtd2 archive_note "Scope and Content" $scope_content]
    append ::entries [mtd2 archive_note "History" $bio_org_history]
    append ::entries [mtd2 archive_note "Provenance" $provenance]
    append ::entries "</entry>\n"
}

proc handlePhysicalDescription {phys_desc} {
    foreach tok [split $phys_desc ,] {
	set tok [string trim $tok]
	if {$tok==""} continue;
	set sentence ""
	set _tok [string tolower $tok]
	if {[regexp {paper} $_tok]} {set tok {Paper Copies}}
	if {[regexp {oversize} $_tok]} {set tok {Oversized Item}}	
	if {[regexp {mp4} $_tok]} {set tok {MP4 File}}
	if {[regexp {mp3} $_tok]} {set tok {MP3 File}}
	if {[regexp {photo} $_tok]} {set tok {Photograph}}
	if {[regexp {cassette} $_tok]} {set tok {Analogue Cassette Tape}}
	if {[regexp {casette} $_tok]} {set tok {Analogue Cassette Tape}}
	if {[regexp {cd disc} $_tok]} {set tok CD}
	if {[regexp {cd media} $_tok]} {set tok CD}	    	
	if {[regexp {cd-r} $_tok]} {set tok CD}	    	    
	foreach word  [split $tok] {
	    append sentence " "
	    set word [string trim [string totitle $word]]
	    set _word [string tolower $word]
	    if {$_word == "cd"} {set word CD}
	    if {$_word == "cds"} {set word CD}
	    if {[string length $word]<=3} { set word [string toupper $word]}
	    append sentence $word
	}

	set tok [string trim $sentence]
	if {$tok=="Audio Fies"} {set tok "Audio File"}
	append ::entries [mtd1 archive_physical_media $tok]
    }
}


proc handleDate {date} {
    set date [clean $date]
    if {$date!="" && $date!="--"} {
	regsub -all s $date {} date
	regsub -all {^\?\-}  $date {} date	
	if {[regexp {(\d\d\d\d) *- *(\d\d\d\d)} $date match date1 date2]} {
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    
	} elseif {[regexp {^(\d\d\d\d)$} $date match date1]} {
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    
	} elseif {[regexp {^(\d\d\d\d)(\d\d\d\d)$} $date match date1 date2]} {
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    
	} else {
	    puts stderr "bad date: $date"
#	    append ::entries [col fromdate $date]
	}
    }
}




set ::sp {collection_nbr series_nbr series_title location scope_content notes user_1 bio_org_history organiz_arrange provenance bulk_dates name_type creator summary column1}
proc series $::sp {
    incr ::scnt
    foreach p $::sp {set $p [string trim [set $p]]}
    set parent [cid $collection_nbr]
##    puts  "parent: $::cmap($parent)"
    set id [sid $collection_nbr $series_nbr]
    set ::sids($id) 1
    append ::entries [openEntry type_archive_series $id $parent  $series_title]
    append ::entries [col series_number [pad $series_nbr]]
    append ::entries [col location $location]
    append ::entries [mtd2 archive_note Scope $scope_content]
    append ::entries [mtd2 archive_note Note $notes]    
    append ::entries [mtd2 archive_note History $bio_org_history]
    append ::entries [mtd2 archive_note Arrangement $organiz_arrange]
    append ::entries [mtd2 archive_note "Provenance" $provenance]
    append ::entries [mtd2 archive_note "Note" $column1]
    append ::entries [mtd2 archive_note "Note" $summary]        
    append ::entries [mtd1 archive_creator $creator]    
    handleDate $bulk_dates
    if {[regexp {.*\d\d\d\d-.*} $name_type]} {
	handleDate $name_type
    } else {
	if {$name_type!=""} {
##	    puts stderr "name_type:$name_type"
	}
    }
    append ::entries "</entry>\n"
}


set ::fp {collection_nbr series_nbr file_unit_nbr title location phys_desc dates summary_note notes_prov creator language}
proc files $::fp {
    if {$file_unit_nbr==""} return
    incr ::fcnt
#    if {$::fcnt>100} return
    foreach p $::fp {set $p [string trim [set $p]]}
    set parent [sid $collection_nbr $series_nbr]
    if {![info exists   ::sids($parent)]} {
	puts  "file: $file_unit_nbr $title no parent series: $parent "
    }
    set id [fid $collection_nbr $series_nbr $file_unit_nbr]
    set ::fids($id) 1
    append ::entries [openEntry type_archive_file $id $parent $title]
    append ::entries [col file_number [pad $file_unit_nbr]]
    append ::entries [col location $location]
    handlePhysicalDescription $phys_desc 
    handleDate  $dates
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]
    append ::entries [mtd1 archive_creator $creator]
    append ::entries [mtd1 archive_language $language]    
    append ::entries "</entry>\n"
}


set ::ip {collection_nbr series_nbr file_unit_nbr item_nbr title location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1}
proc item $::ip {
    incr ::icnt
    if {[string length $location]>300} {
#	puts "$collection_nbr $series_nbr $file_unit_nbr $item_nbr length: [string length $location]"
#	puts "$title $location"
#	exit
    }
    foreach p $::ip {set $p [string trim [set $p]]}
    if {$item_nbr==""} return
    set parent [fid $collection_nbr $series_nbr $file_unit_nbr]
    if {![info exists   ::fids($parent)]} {
#	puts   "no parent for item: $item_nbr $title  parent file: $parent "
	set ::fids($parent) 1
	puts   "files $collection_nbr $series_nbr $file_unit_nbr \{File $file_unit_nbr\}  {}  {}  {}  {}  {}  {}  {}"
	return
    }


    set id [iid $collection_nbr $series_nbr $file_unit_nbr $item_nbr]
    append ::entries [openEntry type_archive_item $id $parent $title]
    append ::entries [col item_number [pad $item_nbr]]
    append ::entries [col location $location]
    handlePhysicalDescription $phys_desc 
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]
    append ::entries [mtd2 archive_note "Family Name" $pers_fam_name]
    append ::entries [mtd2 archive_note "Other Term" $other_terms]            

    append ::entries [mtd1 archive_media_type $media_type]
    append ::entries [mtd1 archive_creator $creator]    
    append ::entries [mtd1 archive_category $category]    
    handleDate  $dates
    foreach tok [split $user_1 ,] {
	set tok [string trim $tok]
	if {$tok==""} continue;
	append ::entries [mtd1 archive_subject $tok]    
    }
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries "</entry>\n"
}

source collections.tcl
source series.tcl
source files.tcl
source extra.tcl
source items.tcl

append ::entries "</entries>\n";

puts $::entries

puts stderr "#collections: $::ccnt #series: $::scnt #files: $::fcnt #items: $::icnt"
