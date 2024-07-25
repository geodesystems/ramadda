
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
proc check {what row field s} {
    regsub -all -- {[\[\]\(\)_,-\.]+} $s { } s
    foreach w1 [split $s { }] {
	foreach w2 [split $w1 "\n"] {
	    foreach word [split $w2 ","] {	    
		if {[regexp {[^\x00-\x7F]} $word]} {
		    if {![info exists ::checkseen($word)]} {
			set ::checkseen($word) 1
			puts "$what - #$row - $field: $word"
		    }
		}
	    }
	}
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


