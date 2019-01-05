package require tdom

#state {AL}  {Alabama}  {01}  {33.001471}  {-86.766233} 
proc state {abbr name fips lat lon} {
    set ::abbrToFips($abbr) $fips
}
source dict.states.tcl
set doc [dom parse [read [open  MemberData2019.xml r]]]
set root [$doc documentElement]
proc getText {textNode} {
    if {$textNode == ""} {return ""}
    return [$textNode nodeValue]
}

puts "map.fields=party,member,url,memberimage"
puts "map.key=geoid"

foreach member [$root selectNodes //member] {
    set district [getText [$member selectNodes {statedistrict/text()}]]
    set infoNode [$member selectNodes {member-info}]
    set name [getText [$infoNode selectNodes official-name/text()]]
    set id [getText [$infoNode selectNodes bioguideID/text()]]
    set party [getText [$infoNode selectNodes party/text()]]
    regexp {(..)(..)} $district match abbr num
    if {![info exists ::abbrToFips($abbr)]} {
        puts stderr "no fips $district - $name - $party"
        continue
    }
    set fips $::abbrToFips($abbr)
    puts "map.${fips}$num.party=$party"
    puts "map.${fips}$num.member=$name"
    set img "https://clerkpreview.house.gov/content/assets/img/members/$id.jpg"
    set url "https://clerkpreview.house.gov/members/$id"
    puts "map.${fips}$num.memberimage=$img"
    puts "map.${fips}$num.url=$url"
}




