package require tdom

#state {AL}  {Alabama}  {01}  {33.001471}  {-86.766233} 
proc state {abbr name fips lat lon} {
    set ::abbrToFips($abbr) $fips
}
source dict.states.tcl
set doc [dom parse [read [open  MemberData.xml r]]]
set root [$doc documentElement]
proc getText {textNode} {
    if {$textNode == ""} {return ""}
    return [$textNode nodeValue]
}

puts "fields=party,member"
puts "key=geoid"

foreach member [$root selectNodes //member] {
    set district [getText [$member selectNodes {statedistrict/text()}]]
    set infoNode [$member selectNodes {member-info}]
    set name [getText [$infoNode selectNodes official-name/text()]]
    set party [getText [$infoNode selectNodes party/text()]]
    regexp {(..)(..)} $district match abbr num
    if {![info exists ::abbrToFips($abbr)]} {
        puts stderr "no fips $district - $name - $party"
        continue
    }
    set fips $::abbrToFips($abbr)
    puts "${fips}$num.party=$party"
    puts "${fips}$num.member=$name"
}




