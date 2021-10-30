proc getCsvUrl {url} {
    catch {exec rm tmp.csv}
    catch {exec curl -k $url > tmp.csv} err
    set fp [open tmp.csv]
    set c [read $fp]
    close $fp
    set c
}


set ::cnt 0
set ::tcnt 0
set ::limit 500
 

proc runit {id {name init}} {
    if {[info exists ::seen($id)]} {return}
    set ::seen($id) 1
    puts stderr "run: $id $name"
    set url "https://geodesystems.com/repository/entry/show?entryid=${id}#fortest"
    set failed [catch {exec curl --insecure -f -silent -o /dev/null $url} err]
    if {$failed} {
	puts stderr "Failed $name $id : $err"
	exit
    }
    set url "https://geodesystems.com/repository/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&fields=name,id&showheader=false&showheader=false"
    set csv [getCsvUrl $url]
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	foreach     {name id} [split $line2 ,] break
	regsub -all {[/ .'\",]+} $name _ clean
	if {$id!="41766a05-fdfe-4224-8615-59c24c54241c"} {
	    runit $id $name
	}
    }
}

    

if {[llength $argv] == 0} {
    runit 2e485e95-eb29-44fc-8987-76e6ac74365a
}
foreach id $argv {
    runit $id
}
exit


