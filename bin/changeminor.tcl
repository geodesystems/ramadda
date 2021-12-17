#!/usr/bin/tclsh

proc get {f} {
    set f [string trim $f]
    set fp [open $f r]
    set c [read $fp]
    close $fp
    return $c
}

set file  {~/source/ramadda/build.properties}
set p  [get $file]
regexp {(?m).*version_minor *=(.*?)$} $p match num
set num2 [incr num]
puts [regsub  "version_minor *= *\[0-9\]+" $p "version_minor=$num2" p]

set fp [open $file w]
puts -nonewline $fp $p
close $fp
