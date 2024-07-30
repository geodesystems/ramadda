

set ::terms [open terms.html w]
set ::messy [open messyterms.html w]
puts $::terms  "<body><div style='font-size:70%;font-family:helvetica;line-height:1.2em;'>"
puts $::messy "<body><div style='font-size:70%;font-family:helvetica;line-height:1.2em;'>"


source ../lib.tcl
set ::entries "<entries>\n";



set ::cnt 0 

set ::bp {name_type author call_nbr title item_type item_count publisher pub_year volume phys_char notes barcode other_terms isbn lccn series column1 _1}

array set ::parent {}

proc getParent {title} {
    regsub -all fixme $title {} title
    set title [string trim $title]
    regexp {^(.)} $title p
    set p [string toupper $p]
    set id "Books - etc"
    foreach key {A-D E-H I-L M-P Q-T U-Z} {
	if {[regexp "\[$key\]" $p]} {
	    set id "Books $key"
	}
    }
    if {![info exists ::parent($id)]} {
	set ::parent($id)  1
	append ::entries [openEntry group $id {} $id]
	append ::entries "</entry>\n"
    }
    return $id
}    

proc book $::bp  {
    incr ::cnt
#    if {$::cnt>100} return
    foreach p $::bp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check books $::cnt $p $v
    }

    catch {
	set isbn [format %.0f $isbn]
    }	

    set parent [getParent $title]
    append ::entries [openEntry type_archive_book {} $parent  $title]    



    set year $pub_year
    set year1 ""
    regsub -all "l" $year 1 year
    regsub -all {^[^\d]*(\d\d\d\d).*} $year {\1} year1
    
    set year [string trim $year1]
    if {![regexp {^\d\d\d\d$} $year]} {
	set year ""
    }
    if {[string length $lccn]>20} {
	set other_terms "$other_terms .  $lccn"
	set lccn ""
    }

    if {$year !=""} {
	append ::entries [col fromdate $year]    
	
    }
    if {[regexp Kurz $item_type]} {
	set item_type ""
    }

    append ::entries [col description $notes]    
    append ::entries [col item_type $item_type]
    append ::entries [col item_count $item_count]    
    append ::entries [col call_number $call_nbr]
    append ::entries [col physical_characteristic $phys_char]
    append ::entries [col authors $author]
    append ::entries [col publisher $publisher]
    append ::entries [col isbn $isbn]
    append ::entries [col lccn $lccn]
    append ::entries [col barcode $barcode]    
    append ::entries [col volume $volume]
    append ::entries [col series $series]        
    if {$other_terms!=""} {
	set fp $::terms
	set messy 0
	if {[string length $other_terms]>1000} {
	    set messy 1
	    set fp $::messy
	}

	puts $fp "<div style='font-weight:bold;'>#$::cnt: $title<div style='margin-left:30px;'>Terms: $other_terms</div></div>"
	regsub -all {\t} $other_terms { } other_terms

	regsub -all {(\d+)\.(\d+)} $other_terms {\1_dot\2} other_terms
	foreach dot {{G.P.O.} {St.} {S.D.}  {U.S.} {D. F.}  {Mont.} { ca.} {Hist.} {etc.} {Wyo.} {D. C.} {1st.} { L.} {C. d. } {A. R.} {Neb.} {n B.} {M. R} {d. } {H. } { Co.} { E. C. } { Fla.} { J.,} {Wis.} {regt.} { S.} {-ca. } {N.D. } {J. A. } { C.} { G.} { F.} {N.D.} { fl. } {D.C.} {Minn.} {Feb.} {Jan.} { 1. } { 2. } { Pa.} {7.100} { J. H.} {0.00} {Ft. } {S.D} {U.S } {970.1} {N.Y.}  { A.} {H.H.} {....} { 2. } { 3. } { O.--b. } {N.J.}  {W. } {Vol.} {T.C.} {Dak. } { E. } {T. } { p. } {Soc. } {United States. Army. } {United States. Army.} {J. } {Dept.} {Calif. } {s.n.} {Supt.} {Docs.}} {
	    regsub -all {\.} $dot {\\.} p
	    regsub -all {\.} $dot {_dot} w	
	    regsub -all -- $p $other_terms $w other_terms
	}
	array set seen {}

	foreach term [split $other_terms .] {
	    set term [string trim $term]
	    if {$term==""} continue
	    regsub -all _dot $term . term
	    regsub -all etc $term etc. term
	    regsub -all {etc\.\.} $term etc. term	    
	    regsub {^--} $term {} term
	    set term [fixme $term]
	    if {[info exists seen($term)]} continue
	    set seen($term) 1
	    puts $fp "<div style='padding:2px;border-bottom:1px solid #ccc;white-space: nowrap;margin-left:60px;'> $term</div>\n"
	    if {!$messy} {
		append ::entries [mtd1 archive_term $term]
	    }


	}
	puts $fp "<div style='margin-top:10px;'></div>"
    }
    append ::entries "</entry>\n"
}


catch {exec sh /Users/jeffmc/bin/seesv.sh -template {} {book {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}} {${15}} {${16}} {${17}} }  {\n} {} books.csv > books.tcl}


source books.tcl
append ::entries "</entries>\n";

set fp [open bookentries.xml w]
puts $fp $::entries
close $fp


puts $::terms "</div></body>"
puts $::messy "</div></body>"
close $::terms
close $::messy





