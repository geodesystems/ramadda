##This unjars the jar file in the jetty distribution
##To build the jetty.jar:
##Download from https://www.eclipse.org/jetty/download.php
##Get the 9.4.x release as 10.x+ uses jvm v11
##Extract the jar cd into the dir
##cd jetty-...
##tclsh <path>/unjarjetty.tcl

##Read the generated classpath
puts stderr "making jars.txt"
exec java -jar start.jar --dry-run > jars.txt
set c [read [open jars.txt r]]

if {![regexp -- {-cp +([^ ]+) } $c match cp]} {
   puts stderr "Could not extract cp from jars.txt"
   exit
}

set jars [split $cp ":"]



##remove and make tmp dir
puts stderr "making tmpjar dir"
exec rm -r -f tmpjars
exec mkdir tmpjars
cd tmpjars

foreach line $jars {
    set line [string trim $line]
    if {$line==""} continue;
    if {[regexp {^#} $line]} continue;
    if {![regexp {.jar$} $line]} continue;
    puts "Unjarring: $line"
    set x [exec jar -xvf $line]
#    puts stderr "X:$x"
}


exec rm -r -f META-INF
exec $::env(SHELL) -c {jar -cvf ../jetty.jar *}
cd ..
puts stderr done
