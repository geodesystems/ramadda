#Usage:
#tclsh cssmin.tcl file.css > out.css
#or:
#tclsh cssmin.tcl file.css out.css

proc get {f} {
    set f [string trim $f]
    set fp [open $f r]
    set c [read $fp]
    close $fp
    return $c
}

proc write {f c} {
    set fp [open $f w]
    puts -nonewline $fp $c
    close $fp
}


proc cvrt {files} {
    set f [lrange $files 0 0]
    set contents [get $f]
    regsub   -all {/\*.*?\*/} $contents { }  contents
    regsub   -all {\s\s*} $contents { }  contents
    regsub   -all {\{ } $contents "\{"  contents
    regsub   -all { \}} $contents "\}"  contents
    regsub   -all {; +} $contents ";"  contents
    regsub   -all { *: *} $contents ":"  contents                
    if {[llength $files]==2} {
	write [lrange $files 1 1] $contents
    } else {
	puts $contents
    }

}

cvrt    $argv

