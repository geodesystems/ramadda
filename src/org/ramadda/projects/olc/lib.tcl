

array set ::spellwords {}
proc loadSpelling {} {
    if {[info exists ::spelling]} {
	return;
    }
    set ::spelling {}
    set script_path [info script]
    set script_dir [file dirname $script_path]
    foreach line [split [read [open ../spelling.txt r]] "\n"] {
	set line [string trim $line]
	if {[regexp {^#} $line]} continue
	if {$line=="quit"} break
	foreach {p w} [split $line :] break
	set ::spellwords($w) 1
	lappend ::spelling [list $p $w]
    }
}

proc spell {s} {
    loadSpelling
    set v $s
    set debug [regexp {Loisel} $s]
#    if {$debug} {puts $s}
    foreach tuple $::spelling {
	foreach {pattern with} $tuple break
	if {[regsub -all $pattern $v $with v]} {
	    #puts "\tchange: $pattern to:$with"
	}
    }
    return [fixme $v]
}



set ::xcnt 0
proc fixme {v {debug 0} {label ""}} {
    set tmp $v
    foreach tuple $::spelling {
	foreach {pattern with} $tuple break
	if {$with==""} continue
	regsub -all $with $tmp {XXX} tmp
    }

    if {[regexp {[^\n\x00-\x7F]} $tmp]} {
	regsub -all {fixme +} $v {} v
	set v "fixme $v"
	if {$debug} {
	    incr ::xcnt
	    check2  $label ""  "" $v
	    #	    puts "fix: $v"
	}
    } else {
	regsub -all {fixme fixme} $v {} v
	regsub -all {fixme +} $v {} v
	regsub -all {fixme} $v {} v		
    }
    set v [string trim $v]
    set v
}






proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}



proc  cid {cid} {
    return collection_$cid
}


proc col {name s {clean 1}} {
    if {$s==""} {
	return ""
    }
    if {$clean} {set s [clean $s]}
    return "<$name>[cdata  $s]</$name>\n"
}    


proc pad {n} {
    if {[string length $n]==1} {
	set n "00$n"
    }   elseif {[string length $n]==2} {
	set n "0$n"
    }    
    set n
}    

array set ::checkseen {}
proc check {what row field _s} {
}


array set ::terms {}
proc check2 {what row field _s} {
    set s $_s
    regsub -all -- {[\[\]\(\)_,-\.]+} $s { } s
    set words {}
    set results ""
    foreach w1 [split $s { }] {
	foreach w2 [split $w1 "\n"] {
	    foreach w3 [split $w2 ":"] {
		foreach word [split $w3 ","] {
		    if {[regexp {[^\x00-\x7F]} $word]} {
			if {![info exists ::checkseen($word)] && ![info exists ::spellwords($word)]} {
			    set ::checkseen($word) 1
			    lappend words $word
			    append results "$what"
			    if {$row!=""} {
				append results " - #$row"
			    }
			    append results " - $field: $word\n"
			}
		    }
		}
	    }
	}
    }
    if {![info exists ::addedcss]} {
	puts "title,original phrase,correct phrase"
	if {0} {
	    puts {<!DOCTYPE html>}
	    puts "<html><head><title>$what</title>"
	    puts {<style type="text/css">body {line-height:1.2em;font-family: Helvetica;} </style>}
	    puts {</head><body>}
	    puts "<h1>$what</h1>"
	}
	set ::addedcss 1
    }
    if {$results!=""} {
	puts -nonewline "$what,"
#	puts -nonewline "#$row: "
	set cnt 0
	set _s [string trim $_s]
	foreach w $words {
	    incr cnt
	    if {$cnt>1} {puts -nonewline " - "}
#	    puts -nonewline "<i>$w</i> "
	    regsub -all $w $_s "<b>$w</b>" _s
	}
	if {![info exists ::terms($w)]} {
	    set ::terms($w) 1
	    puts "$w,$w"
	}
#	puts "<div style='border-bottom:1px solid #efefef;max-height:200px;overflow-y:auto;padding:5px;margin:5px;margin-bottom:10px;margin-left:40px;'>$_s</div>"
    }

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
    set e   "<entry  [attr type $type] ";
    if {$id!=""} {
	append e [attr id $id]
    }
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


