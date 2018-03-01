set urls [list]
set ::loop 0 


foreach file $argv {
    if {$file == "-loop"} {
        set ::loop 1
        continue;
    }
    lappend urls "from:$file"
    foreach url [split [read [open $file r]] "\n"]  {
        set url [string trim $url]
        if {$url == ""} {continue;}
        lappend  urls  $url
    }
}


set start [clock milliseconds]
set cnt 0
set cntInterval 0
set errors 0
set max 1
if {$::loop} {
    set max 100
}

for {set i 0} {$i < $max} {incr i} {
    foreach url $urls {
        set url [string trim $url]
        if {$url == ""} {continue;}
        if {$url == "exit"} {
            exit;
        }
        if {[regexp {from:(.*)} $url match from]} {
            puts "Testing $from"
            continue;
        }


        if {[regexp {\#.*} $url]} {
            continue;
        }
        incr cnt
        if {[expr ($cnt%5) == 0]} {
            set end [clock milliseconds]
            set delta [expr $end-$start]
            set minutes [expr ${delta}/1000.0/60.0]
            if {$minutes>0.001} {
                puts "#$cnt urls [format {%.1f} [expr ($end-$start)/1000.0]] seconds [expr int($cnt/$minutes)] urls/minute "
            } else {
                puts "#$cnt urls [format {%.1f} [expr ($end-$start)/1000.0]] seconds  - In no time "
            }
        }

        set expectingFail 0;
        if {[regexp {fail:(.*)} $url match url]} {
            set expectingFail 1;
        }
        set failed [catch {exec curl -f -silent -o test.out $url} err]
        if {$failed} {
            if {$expectingFail} {
#                puts "OK: Was expecting a failure for: $url"
            } else  {
                puts "*** Error (failed): $err   URL: $url"
                incr errors
                if {$errors>10} {
                    puts "Too many errors"
                    exit;
                }
            }
        } else {
            if {$expectingFail} {
                puts "*** Error: Expected failure but got OK: $url"
                incr errors
                if {$errors>10} {
                    puts "Too many errors"
                    exit;
                }
            } 
            set fp [open test.out r]
            set html [read $fp]
            close $fp
            if {0} {
                if {[regexp {(\$\{[^\}]+)\}} $html match macro]} {
                    puts "Got pattern in $url"
                } 
            }
        }
    }
}

if {$cnt == 0} {
    puts "None read"
} else {
    set end [clock milliseconds]
    set delta [expr $end-$start]
    set minutes [expr $delta/1000/60.0]
    if {$minutes>0} {
        puts "$cnt urls in [format {%.1f} [expr $delta/1000.0]] seconds [expr int($cnt/$minutes)] urls/minute "
    } else {
        puts "$cnt urls in [format {%.1f} [expr $delta/1000.0]] seconds  - no time "
    }
}