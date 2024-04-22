
set ::key [expr round(100000000*rand())];
puts $::key
proc getCsvUrl {url} {
    catch {exec rm tmp$::key.csv}
    catch {exec curl -k $url > tmp$::key.csv} err
    set fp [open tmp$::key.csv]
    set c [read $fp]
    close $fp
    set c
}


set ::cnt 0
set ::tcnt 0
set ::limit 500
 
set ::ids [list]


proc processLine {line} {
#	puts "idx: $idx  [llength $lines] line:$line"
    if {$line==""} return;
    foreach     {name id} [split $line ,] break
    regsub -all {[/ .'\",]+} $name _ clean
    if {[regexp {synth:} $id]}  return;
    if {$id!="0d3e3a11-7f83-4a0f-9975-d2bc59021567" && $id!="82704951-f788-420c-ba8e-9ac7567ffebe" && $id!="493b2c22-47fb-41a2-bbba-8a758554613b" && $id!="41766a05-fdfe-4224-8615-59c24c54241c" && 	    $id !="59324c53-f8ab-4d38-a027-3aa8d7fc0513"} {
	recurse $id $name
    }
}

proc recurse {id {name init}} {
    if {[info exists ::seen($id)]} {return}
    set ::seen($id) 1
    puts stderr "$id $name"
    set url "https://ramadda.org/repository/entry/show?entryid=${id}#fortest"
    set failed [catch {exec curl --insecure -f -silent -o /dev/null $url} err]
    if {$failed} {
	puts stderr "Failed $name $id : $err"
	exit
    }
    set url "https://ramadda.org/repository/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&escape=true&fields=name,id&showheader=false"
    set csv [string trim [getCsvUrl $url]]
    set lines [split $csv "\n"]
    while {[llength $lines]>0} {
	set length [llength $lines]
	set idx [expr {int(floor(rand()*$length))}]
	set line [string trim [lindex $lines $idx]]
	set lines [lreplace $lines $idx $idx]
	processLine $line
    }
}


if {[llength $argv] == 0} {
    recurse 2e485e95-eb29-44fc-8987-76e6ac74365a
<}
foreach id $argv {
    recurse $id
}
exit


