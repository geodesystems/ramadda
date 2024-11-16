

proc toProperCase {input} {
    set result {}
    foreach word [split $input] {
        set first [string toupper [string index $word 0]]
        set rest [string tolower [string range $word 1 end]]
        append result "$first$rest "
    }
    return [string trim $result]
}

proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
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

proc closeEntry {} {
    return "</entry>\n"
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
    if {$value1=="" && $value2==""} {return}
    set xml  "<metadata [attr type $type]>\n"
    append  xml "<attr [attr index 1] [attr encoded false]>[cdata [clean $value1]]</attr>"
    if {$value2!=""} {
	append  xml "<attr [attr index 2] [attr encoded false]>[cdata [clean $value2]]</attr>"
    }
    append xml "</metadata>\n"
    set xml
}

proc clean {s} {
    return $s
}

proc  mtd5 {type v1 {v2 {}} {v3 {}} {v4 {}} {v5 {}}} {
    if {$v1=="" && $v2=="" && $v3 == "" && $v4 == "" && $v5==""} {return}
    set xml  "<metadata [attr type $type]>\n"
    append  xml "<attr [attr index 1] [attr encoded false]>[cdata [clean $v1]]</attr>"
    if {$v2!=""} {
	append  xml "<attr [attr index 2] [attr encoded false]>[cdata [clean $v2]]</attr>"
    }
    if {$v3!=""} {
	append  xml "<attr [attr index 3] [attr encoded false]>[cdata [clean $v3]]</attr>"
    }    
    if {$v4!=""} {
	append  xml "<attr [attr index 4] [attr encoded false]>[cdata [clean $v4]]</attr>"
    }
    if {$v5!=""} {
	append  xml "<attr [attr index 5] [attr encoded false]>[cdata [clean $v5]]</attr>"
    }

    append xml "</metadata>\n"
    set xml
}


proc  mtdN {type args} {
    set xml  "<metadata [attr type $type]>\n"
    for {set i 0} {$i<15} {incr i} {
	set v [lindex $args $i]
	if {$v!=""} {
	    append  xml "<attr [attr index [expr $i+1]] [attr encoded false]>[cdata [clean $v]]</attr>"
	}
    }
    append xml "</metadata>\n"
    set xml
}



