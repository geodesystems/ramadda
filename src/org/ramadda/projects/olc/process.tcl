
source ../lib.tcl
set ::entries "<entries>\n";
array set ::cids {}
array set ::sids {}
array set ::fids {}

set ::ccnt 0
set ::scnt 0
set ::fcnt 0
set ::icnt 0
array set ::cmap {}

array set ::names {}
proc name {name} {
    set ::names($name) 1
}
source names.tcl

array set ::keywords {}
proc keyword {keyword} {
    set ::keywords($keyword) 1
}
source keywords.tcl


proc processSummary {summary} {
    set summary [clean $summary]
    if {[regexp -nocase {Names included in this collection:(.*)$} $summary match names]} {
	set summary [string map [list $match { }] $summary]
	regsub -all {\n} $names { } names
	processSubjectsAndKeywords $names
    }
    if {[regexp -nocase {Keywords:(.*)$} $summary match keywords]} {
	set summary [string map [list $match { }] $summary]
	regsub -all {\n} $keywords { } keywords
	processSubjectsAndKeywords $keywords
    }
    if {[regexp  {Keywords [^:]+:(.*)$} $summary match keywords]} {
	regsub -all {\([^\)]+\)} $keywords { }  keywords
	set summary [string map [list $match { }] $summary]
	regsub -all  $match $summary { } summary
	regsub -all {\n} $keywords { } keywords
	processSubjectsAndKeywords $keywords
    }    

    return $summary
}

proc processSubjectsAndKeywords {v} {
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
	    append ::entries [mtd1 archive_media_type $tok]
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
	    append ::entries [mtd2 archive_note Note $tok]
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
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check collection $::ccnt $p $v
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
	    puts stderr "bad date: $date what: $what #$idx"
#	    append ::entries [col fromdate $date]
	}
    }
}




set ::sp {collection_nbr series_nbr series_title location scope_content notes user_1 bio_org_history organiz_arrange provenance bulk_dates name_type creator summary column1}
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
    set ::sids($id) 1
    append ::entries [openEntry type_archive_series $id $parent  $series_title]
    set summary [processSummary $summary]
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


set ::fp {collection_nbr series_nbr file_unit_nbr title location phys_desc dates summary_note notes_prov creator language}
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
    set ::fids($id) 1
    append ::entries [openEntry type_archive_file $id $parent $title]
    set summary_note [processSummary $summary_note]

    append ::entries [col file_number [pad $file_unit_nbr]]
    append ::entries [col location $location]
    handlePhysicalDescription $phys_desc 
    handleDate  $dates file $::fcnt
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]
    append ::entries [mtd1 archive_creator $creator]
    append ::entries [mtd1 archive_language $language]    
    append ::entries "</entry>\n"
}

array set ::seen {}


set ::ip {collection_nbr series_nbr file_unit_nbr item_nbr title location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1}
proc item $::ip {
    incr ::icnt
    if {[string length $location]>300} {
#	puts "$collection_nbr $series_nbr $file_unit_nbr $item_nbr length: [string length $location]"
#	puts "$title $location"
#	exit
    }
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
    handleDate  $dates item $::icnt
#    puts $user_1
    processSubjectsAndKeywords $user_1
    set summary_note [processSummary $summary_note]
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries "</entry>\n"
}

source collections.tcl
source series.tcl
source files.tcl
source extra.tcl
source items.tcl

append ::entries "</entries>\n";
set fp [open archiveentries.xml w]
puts $fp $::entries
close $fp
#puts $::entries
puts stderr "#collections: $::ccnt #series: $::scnt #files: $::fcnt #items: $::icnt"



