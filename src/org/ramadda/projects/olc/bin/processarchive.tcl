
source ../lib.tcl
set ::entries "<entries>\n";
array set ::cids {}
array set ::sids {}
array set ::fids {}
array set ::iids {}


set ::ccnt 0
set ::scnt 0
set ::fcnt 0
set ::icnt 0

set ::m_ccnt 0
set ::m_scnt 0
set ::m_fcnt 0
set ::m_icnt 0


array set ::cmap {}

source ../names.tcl
source ../keywords.tcl
source media.tcl

proc makeNote {note} {
    return  [mtd1 archive_internal $note]

}

proc cleanType {type} {
    if {$type=="PHOTOGRAPH"} {set type Photograph}
    set type
}

proc processSummary {summary {from Summary}} {
    set summary [clean $summary]
    if {[regexp -nocase {Names included in this collection:(.*)$} $summary match names]} {
	set summary [string map [list $match { }] $summary]
	regsub -all {\n} $names { } names
	processSubjectsAndKeywords $names $from
    }
    if {[regexp -nocase {Keywords:(.*)$} $summary match keywords]} {
	set summary [string map [list $match { }] $summary]
	regsub -all {\n} $keywords { } keywords
	processSubjectsAndKeywords $keywords $from
    }
    if {[regexp  {Keywords [^:]+:(.*)$} $summary match keywords]} {
	regsub -all {\([^\)]+\)} $keywords { }  keywords
	set summary [string map [list $match { }] $summary]
	regsub -all  $match $summary { } summary
	regsub -all {\n} $keywords { } keywords
	processSubjectsAndKeywords $keywords $from
    }    

    return $summary
}

proc processSubjectsAndKeywords {v {from {Subjects and Keywords}}} {
    set v [clean $v]
    regsub -all {\n} $v { }  v
    regsub -all {Interview with} $v {, } v
    regsub -all {Biographical Sketch:} $v { } v
    regsub -all {Chapters 3 and 4:} $v { } v
    regsub -all {Ricker Tablets: Touch the Clouds} $v {, Ricker Tablets: Touch the Clouds,} v
    regsub -all {Soldiers of the Plains:} $v {, Soldiers of the Plains,} v
    regsub -all {Chapter XXIV: Red Cloud} $v {,Chapter XXIV: Red Cloud, } v
    regsub -all {Museum of the Fur Trade Quarterly} $v {,Museum of the Fur Trade Quarterly,} v
    regsub -all {the age at enlistment (not always given)} $v { } v
    regsub -all {Sioux Fires Burn Again:} $v { } v
    regsub -all {Keywords tied to this audio for research} $v {,} v
    regsub -all {A Troublesome Guest:} $v { } v
    regsub -all {Keywords for entire series} $v Keywords v
    regsub -all {Keywords +:} $v Keywords: v
    regsub -all {Keyword *:} $v Keywords: v
    regsub -all {Interpreters:} $v {, } v
    regsub -all {Interviewees \(includes transcripts\):} $v { } v
    regsub -all -nocase {Mrs\.,} $v {Mrs. } v
    regsub -all -nocase {In addition to names mentioned above:} $v { } v
    regsub -all -nocase {Names of People.*?:} $v { } v
    regsub -all -nocase {and many, many other names.} $v { } v
    regsub -all {  +} $v { } v
    set interviewer ""
    if {[regexp {.*Interviewer:(.*) *Names} $v match interviewer]} {
	regsub "Interviewer:$interviewer" $v {} v
	set interviewer [string trim $interviewer]
    } elseif {[regexp {.*Interviewer:(.*) *Keywords} $v match interviewer]} {
	regsub "Interviewer:$interviewer" $v {} v
	set interviewer [string trim $interviewer]
    } elseif {[regexp {.*Interviewer:(.*)$} $v match interviewer]} {
	regsub "Interviewer:$interviewer" $v {} v
	set interviewer [string trim $interviewer]
    }
    set v [string trim $v]
    if {$v==""} return
    set what KEYWORD
    if {[regexp -nocase {keywords:} $v]} {
	set what KEYWORD
	regsub  -nocase {keywords:} $v {} v
    } elseif {[regexp -nocase {names mentioned:} $v]} {
	set what SUBJECT
	regsub  -nocase {names mentioned:} $v {} v
    }
    regsub -all {;} $v {,} v
    foreach tok [split $v ,] {
	set tok [fixme $tok]
	set _what $what
	set tok [string trim $tok]
	if {$tok=="Disc"} {
	    append ::entries [mtd1 archive_media_description [cleanType $tok]]
	    continue;
	}
	regsub -all {"} $tok {} tok
	regsub -all {\.$} $tok {} tok

	if {[string length $tok]<=3} {continue}
	if {[info exists ::names($tok)]} {
	    set _what SUBJECT
	} elseif {[info exists ::keywords($tok)] || $tok=="1890"} {
	    set _what KEYWORD
	}
	if {[string length $tok]>40} {
	    set _what NOTE
	}
	if {$_what=="KEYWORD"} {
	    append ::entries [mtd1 archive_keyword $tok]
	} elseif {$_what=="SUBJECT"} {
	    regsub -all "^the" $tok {} tok
	    regsub -all "^and" $tok {} tok	    
	    set tok [string trim $tok]
	    append ::entries [mtd1 archive_subject $tok]
	} else {
	    append ::entries [makeNote $tok]
	}

	if {![info exists ::seen($tok)]} {
	    set ::seen($tok) 1
#	    puts "$_what: $tok"
	    if {$_what=="KEYWORD"} {
#		puts "name {$tok}"
	    }
	}
    }
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



proc makeId {id} {
    set i {}
    foreach tok [split $id .] {
	set tok [string trim $tok]
	while {[string length $tok]<3} {
	    set tok "0$tok"
	}
	lappend i $tok
    }
    set id [join $i .]
#    puts $id
    set id
}

proc  cid {cid} {
    set cid [makeId $cid]
    regsub -all {\.} $cid _ cid
    return $cid
}

proc  sid {cid sid} {
    set sid [makeId $sid]
    return "[cid $cid].$sid"
}

proc  fid {cid sid fid} {
    set fid [makeId $fid]
    return "[sid $cid $sid].$fid"
}

proc  iid {cid sid fid iid} {
    set iid [makeId $iid]
    return "[fid $cid $sid $fid].$iid"
}

set ::cp {collection_title collection_nbr shelf_location organiz_arrange scope_content bio_org_history user_1 notes provenance column1 _1 _2 _3}
proc collection $::cp  {
    incr ::ccnt
    foreach p $::cp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check collection $::ccnt $p $v
    }

    set cid [cid $collection_nbr]
    set ::cmap($cid) $collection_title
    set ::cids($cid) $collection_title
    append ::entries [openEntry type_archive_collection $cid "" $collection_title]
    append ::entries [col collection_number [pad $collection_nbr]]
    append ::entries [col shelf_location $shelf_location]
#    puts "$collection_title  $column1"
    append ::entries [makeNote $column1]
    append ::entries [mtd2 archive_note Arrangement $organiz_arrange]
    append ::entries [mtd2 archive_note "Scope and Content" $scope_content]
    append ::entries [mtd2 archive_note "Creator Sketch" $bio_org_history]
    append ::entries [mtd2 archive_note "Provenance" $provenance]
    append ::entries "</entry>\n"
}

proc handlePhysicalDescription {phys_desc} {
    foreach tok [split $phys_desc ,] {
	set tok [fixme $tok]
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
	    set word [fixme $word]
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


proc handleDate {date what idx} {
    set date [clean $date]
    regsub -all 19696 $date 1969 date
    regsub {\? - 2017} $date 2017 date
    regsub {1957 & } $date {} date
    regsub {08/27/966} $date 08/27/1966 date
    if {$date=="?"} return
    if {$date!="" && $date!="--"} {
	regsub -all s $date {} date
	regsub -all {^\?\-}  $date {} date	
	if {[regexp {^(\d\d?)/(\d\d?)/(\d\d\d\d)$} $date match mm dd yyyy]} {
	    set date1 "${yyyy}-${mm}-${dd}"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    
	} elseif {[regexp {10/08/1987, 10/27/1987, 11/2/1987} $date]} {
	    append ::entries [col fromdate 1988-10-08]
	    append ::entries [col todate 1988-11-02]	    
	}  elseif {[regexp {2/1-2/26/88} $date]} {
	    append ::entries [col fromdate 1988-02-01]
	    append ::entries [col todate 1988-02-26]	    
	} elseif {[regexp {^(\d\d?)/(\d\d?)/(\d\d)$} $date match mm dd yy]} {
	    set yyyy "19$yy"
	    set date1 "${yyyy}-${mm}-${dd}"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]
	} elseif {[regexp {^(\d\d?)-(\d\d?)-(\d\d)$} $date match mm dd yy]} {
	    set yyyy "19$yy"
	    set date1 "${yyyy}-${mm}-${dd}"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]
	} elseif {[regexp {^(\d\d\d\d) *, *(\d\d\d\d)$} $date match y1 y2]} {
#	    puts "DATE: $y1 - $y2"
	    set date1 "${y1}-01-01"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]
	} elseif {[regexp {1976\?} $date match]} {
	    set date1 "1976-01-01"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    	    
	} elseif {[regexp {^(\d\d?)-(\d\d?)-(\d\d\d\d)$} $date match mm dd yyyy]} {
	    set date1 "${yyyy}-${mm}-${dd}"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    	    
	} elseif {[regexp {^(\d\d?)/(\d\d\d\d)$} $date match mm yyyy]} {
	    set dd 01
	    set date1 "${yyyy}-${mm}-${dd}"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    	    
	} elseif {[regexp {^xxx(\d\d?)-22/23-75$} $date match yyyy]} {
	    set date1 "${yyyy}-06-01"
	    set date2 "${yyyy}-09-01"	    
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    
	} elseif {[regexp {^Summer.*(\d\d\d\d)$} $date match yyyy]} {
	    set date1 "${yyyy}-06-01"
	    set date2 "${yyyy}-09-01"	    
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    
	} elseif {[regexp {^(June|September|December).*(\d\d\d\d)$} $date match m yyyy]} {
	    set mm 06
	    if {$m=="September"} {set mm 09}
	    if {$m=="December"} {set mm 12}	    
	    set date1 "${yyyy}-$mm-01"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    	    
	} elseif {[regexp {(\d\d\d\d) *- *(\d\d\d\d)} $date match date1 date2]} {
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    
	} elseif {[regexp {^(\d\d\d\d)$} $date match date1]} {
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date1]	    
	} elseif {[regexp {^(\d\d\d\d)(\d\d\d\d)$} $date match date1 date2]} {
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]


	} elseif {[regexp {^(\d\d?)-(\d\d?)/(\d\d?)-(\d\d\d?\d?)$} $date match m d1 d2 y ]} {
	    if {[string length $y]==2} {
		set y "19$y"
	    }
	    set date1 "$y-$m-$d1"
	    set date2 "$y-$m-$d2"	    
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    
	} elseif {[regexp {^(\d\d\d\d) +(\d\d\d\d)} $date match y1 y2]} {
	    set date1 "$y1-01-01"
	    set date2 "$y2-12-30"
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    

	} elseif {[regexp {^(\d\d?)/(\d\d?)/(\d\d\d\d) *- *(\d\d?)/(\d\d?)/(\d\d\d\d)} $date match m1 d1 y1 m2 d2 y2 ]} {
	    set date1 $y1-$m1-$d1
	    set date2 $y2-$m2-$d2	    
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    
	} elseif {[regexp {^(\d\d?)/(\d\d?)-(\d\d?)/(\d\d\d?\d?)$} $date match m d1 d2 y ]} {
	    if {[string length $y]==2} {
		set y "19$y"
	    }
	    set date1 "$y-$m-$d1"
	    set date2 "$y-$m-$d2"	    
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    
	    
	} elseif {[regexp {^1950'.*$} $date match]} {
	    set date1 1950-01-01
	    set date2 1959-12-30
	    append ::entries [col fromdate $date1]
	    append ::entries [col todate $date2]	    	    
	} else {
	    puts  "bad date: $what :$date:"
#	    append ::entries [col fromdate $date]
	}
    }
}




set ::sp {collection_nbr series_nbr series_title shelf_location scope_content notes user_1 bio_org_history organiz_arrange provenance bulk_dates name_type creator summary column1}
proc series $::sp {
    incr ::scnt
    foreach p $::sp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check series $::scnt $p $v
    }
    set parent [cid $collection_nbr]
##    puts  "parent: $::cmap($parent)"
    set id [sid $collection_nbr $series_nbr]
    set ::sids($id) $series_title
    append ::entries [openEntry type_archive_series $id $parent  $series_title]
    set from "series: $series_title"
    set summary [processSummary $summary $from]
    append ::entries [col series_number [pad $series_nbr]]
    append ::entries [col shelf_location $shelf_location]
    append ::entries [mtd2 archive_note Scope $scope_content]
    
    append ::entries [makeNote $notes]    
    append ::entries [mtd2 archive_note "Creator Sketch" $bio_org_history]
    append ::entries [mtd2 archive_note Arrangement $organiz_arrange]
    append ::entries [mtd2 archive_note "Provenance" $provenance]
    append ::entries [makeNote $column1]
    append ::entries [makeNote $summary]        
    append ::entries [mtd1 archive_creator $creator]    
    handleDate $bulk_dates series $::scnt
    if {[regexp {.*\d\d\d\d-.*} $name_type]} {
	handleDate $name_type series $::scnt
    } else {
	if {$name_type!=""} {
##	    puts stderr "name_type:$name_type"
	}
    }
    append ::entries "</entry>\n"
}


set ::fp {collection_nbr series_nbr file_unit_nbr title shelf_location phys_desc dates summary_note notes_prov creator language}
proc files $::fp {
    if {$file_unit_nbr==""} return
    incr ::fcnt
    foreach p $::fp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check file $::fcnt $p $v
    }
    set parent [sid $collection_nbr $series_nbr]
    if {![info exists   ::sids($parent)]} {
	puts  stderr "file: $file_unit_nbr $title no parent series: $parent "
    }
    set id [fid $collection_nbr $series_nbr $file_unit_nbr]
    set ::fids($id) $title
    append ::entries [openEntry type_archive_file $id $parent $title]
    set summary_note [processSummary $summary_note "file: $title"]

    append ::entries [col file_number [pad $file_unit_nbr]]
    append ::entries [col shelf_location $shelf_location]
    handlePhysicalDescription $phys_desc 
    handleDate  $dates file $::fcnt
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]
    append ::entries [mtd1 archive_creator $creator]
    append ::entries [mtd1 archive_language $language]    
    append ::entries "</entry>\n"
}

array set ::seen {}


set ::ip {collection_nbr series_nbr file_unit_nbr item_nbr title shelf_location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1}
proc item $::ip {
    foreach p $::ip {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check item $::icnt $p $v
    }
    if {$item_nbr==""} return
    set parent [fid $collection_nbr $series_nbr $file_unit_nbr]
    if {![info exists   ::fids($parent)]} {
	set ::fids($parent) 1
	puts   stderr "files $collection_nbr $series_nbr $file_unit_nbr \{File $file_unit_nbr\}  {}  {}  {}  {}  {}  {}  {}"
	return
    }

    incr ::icnt
    set id [iid $collection_nbr $series_nbr $file_unit_nbr $item_nbr]
    set ::iids($id) $title
    append ::entries [openEntry type_archive_item $id $parent $title]
    append ::entries [col item_number [pad $item_nbr]]
    append ::entries [col shelf_location $shelf_location]
    handlePhysicalDescription $phys_desc 
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]
    append ::entries [mtd2 archive_note "Family Name" $pers_fam_name]
    append ::entries [mtd2 archive_note "Other Term" $other_terms]            




    set media_type [cleanType $media_type]
    append ::entries [mtd1 archive_media_description $media_type]
    append ::entries [mtd1 archive_creator $creator]    
    append ::entries [mtd1 archive_category $category]    
    handleDate  $dates item $::icnt
#    puts $user_1
    processSubjectsAndKeywords $user_1 "item: $title <span style='color:blue;'>field:user_1</span>"
    set summary_note [processSummary $summary_note]
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries "</entry>\n"
}

source collections.tcl
source media_collection.tcl
source series.tcl
source media_series.tcl
source files.tcl
source media_file.tcl
source extra.tcl
source items.tcl
source media_item.tcl

append ::entries "</entries>\n";
set fp [open archiveentries.xml w]
puts $fp $::entries
close $fp
#puts $::entries
puts stderr "#collections: $::ccnt #series: $::scnt #files: $::fcnt #items: $::icnt #media items: $::m_icnt"



#puts "</table>"
