

puts "<entries>"
set cnt 0
foreach file $argv {
    set fp [open $file r]
    set c [read $fp]
    close $fp

    while {[regexp {.*?(<TR.*?>view<.*?</TR>)(.*)$} $c match blob c]} {
        incr cnt
        if {$cnt>50} break;
#        regsub -all {<T[^>]+>} $blob {_} blob
#        regsub -all {<I[^>]+>} $blob {_} blob
#        regsub -all {</[^>]+>} $blob {_} blob
        regsub -all {\n} $blob { } blob

        regsub -all {<TD WIDTH=10>&nbsp;</TD>} $blob {} blob
#        puts "BLOB: $blob"
        set colp {<TD[^>]*>([^(</TD>)]+)</TD>\s*}
        set colp {<TD[^>]*>(.*?)</TD>\s*}
        if {[regexp "${colp}${colp}${colp}${colp}${colp}${colp}" $blob match cas formula name state htmlUrl links]} {
            regsub -all {<[^>]+>} $formula {} formula
            regexp {HREF=([^>]+)>}  $htmlUrl match htmlUrl
            regsub -all {\.html} $htmlUrl {.txt} txtUrl
            set txtUrl "http://kinetics.nist.gov/janaf/$txtUrl"
            set file [file tail $txtUrl]
#            puts "curl -o $file \"$url\""
            set name "$name - $file"
            puts stderr $file
            puts "<entry type=\"type_nist_janaffile\" name=\"$name\" formula=\"$formula\" url=\"http://kinetics.nist.gov/janaf/$htmlUrl\" cas_number=\"$cas\" state=\"$state\" file=\"$file\"/>"
#            puts "cas=$cas formula=$formula name=$name state=$state url=$url "
        } else {
            puts "MISS $blob"
        }
#        puts "c: $c"
    }
}

puts "</entries>"