set sleep 2
set steps 34
set delay 50
set output capture.gif
set top 200
set left 120
set bottom 150
set right 125

set ::loc [file dirname [file normalize [info script]]]
for {set i 0} {$i<[llength $argv]} {incr i} {
    switch  [lindex $argv $i] {
	-o {
	    set output [lindex $argv [incr i]]
	}
	-top {
	    set top [lindex $argv [incr i]]
	}
	-right {
	    set right [lindex $argv [incr i]]
	}
	-left {
	    set left [lindex $argv [incr i]]
	}
	-bottom {
	    set bottom [lindex $argv [incr i]]
	}
	-steps {
	    set steps [lindex $argv [incr i]]
	}
	-sleep {
	    set sleep [lindex $argv [incr i]]
	}
	-delay {
	    set delay [lindex $argv [incr i]]
	}
	default {
	    puts stderr "usage: -o <output file> -steps <number of steps> -sleep <sleep seconds> -delay <hundredth's second> -top <top chop> -left <left chop> -right <right chop> -bottom <bottom chop>"
	    exit
	}
    }
}

for {set i 0} {$i<$steps} {incr i} {
    set thumb capture${i}.png
    if {$i<10} {
	set thumb capture0${i}.png
    }
    if {[catch {
	if {$i>0} {
	    exec osascript $::loc/animationstep.scpt
	    exec sleep $sleep
	}
	exec osascript -e {activate application "Safari"}
	exec osascript $::loc/../test/capture.scpt
	puts stderr $thumb
	exec mv capture.png $thumb
    } err]} {
	puts stderr "Error: $err"
	exit
    }
}


puts stderr "making $output"
exec convert -delay $delay -loop 0 capture*.png -chop ${left}x${top}  -gravity south -chop 0x${bottom} -gravity east -chop ${right}x0 $output
puts "done"
