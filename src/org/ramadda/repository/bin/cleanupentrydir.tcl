
set ::total 0
proc walk {dir} {
    if {[file isdirectory $dir]} {
        set name [file tail $dir]
        if {$name != "entries" && ![regexp {^entry_} $name]} {
            if {![info exists ::entry($name)]} {
		set duout [exec du -s  $dir]
		regexp {^([^\s]+)\s+} $duout match size
		incr ::total $size
                puts "$dir"
            }
        }
        foreach child [glob -nocomplain [file join $dir *]] {
            walk $child
        }
    } else {
#        puts "file: $dir"
    }
}


puts [lindex $argv 0]
set c [read [open [lindex $argv 0] r]]
foreach line [split $c "\n"] {
    foreach {id type name} [split $line ,] break
    set ::entry($id) $name
}



set cnt 0
foreach file $argv {
    incr cnt
    if {$cnt == 1} continue
    walk $file
}

##puts "SIZE: [expr $::total/1000]"