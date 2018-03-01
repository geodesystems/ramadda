
puts "<entries>"
#CA001017230 SHAWNIGAN LAKE            48.6500 -123.6333  138.0 1911 2016
set ::cnt 0
foreach line  [split [read [open us.dly_ppt.1901-2016.hd r]] "\n"] {
        if {[regexp {^([^\s]+)\s+(.*)\s+([\d\.-]+)\s+([\d\.-]+)\s+([\d\.]+)\s+([\d]+)\s+([\d]+)$} $line match id name lat lon alt start end]} {
            set name [string trim $name]
## uncomment for camel-case
##            set nm ""
##            foreach tok [split $name " "] {
##                append nm [string totitle $tok]
##                append nm " "
##            }
##            set name [string trim $nm]
            regsub -all "&" $name {&amp;} name
            puts "<entry type=\"type_psd_station\" name=\"$name\" latitude=\"$lat\" longitude=\"$lon\" altitude=\"$alt\" fromdate=\"$start-01-01\" todate=\"$end-01-01\" station_id=\"$id\"/>"
##            if {[incr ::cnt] >10} break
        }  else {
#            puts "failed $line"
        }
}
puts "</entries>"
