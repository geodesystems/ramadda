set ::loc [file dirname [file normalize [info script]]]
set sleep 2
set steps 1
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
	puts $thumb
	exec mv capture.png $thumb
    } err]} {
	puts stderr "Error: $err"
	exit
    }
}

puts "making gif"
exec convert -delay 100 -loop 0 capture*.png capture.gif
puts "done"
