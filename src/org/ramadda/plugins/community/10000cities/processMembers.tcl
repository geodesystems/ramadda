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

puts "map.fields=party,state,member,url,memberimage,town,phone,address"
puts "map.key=geoid"

foreach member [$root selectNodes //member] {
    set district [getText [$member selectNodes {statedistrict/text()}]]
    set infoNode [$member selectNodes {member-info}]
    set state [getText [$infoNode selectNodes {state/state-fullname/text()}]]
    set name [getText [$infoNode selectNodes official-name/text()]]
    if {$name == ""} {
        set name "[getText [$infoNode selectNodes firstname/text()]] [getText [$infoNode selectNodes lastname/text()]]"
    }
    set town [getText [$infoNode selectNodes townname/text()]]
    set office [getText [$infoNode selectNodes office-building/text()]]
    set room [getText [$infoNode selectNodes office-room/text()]]
    set zip "[getText [$infoNode selectNodes office-zip/text()]]-[getText [$infoNode selectNodes office-zip-suffix/text()]]"
    set phone [getText [$infoNode selectNodes phone/text()]]

    switch ${office}  {
        LHOB {set office  "Longworth House Office Building" }
        CHOB {set office "Cannon House Office Building "}
        RHOB {set office "Rayburn House Office Building"}
    }
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
    puts "map.${fips}$num.state=$state"
    set img "https://clerkpreview.house.gov/content/assets/img/members/$id.jpg"
    set url "https://clerkpreview.house.gov/members/$id"
    puts "map.${fips}$num.memberimage=$img"
    puts "map.${fips}$num.url=$url"
    puts "map.${fips}$num.town=$town"
    puts "map.${fips}$num.phone=$phone"
    puts "map.${fips}$num.address=$room $office<br>Washington, DC, $zip"
}





