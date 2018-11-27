

proc state {abbr name fips lat long} {
    puts "kml.statefp.$fips=$name"
}



proc county {state geoid ansi name pop hu aland awater aland_sqmi awater_sqmi lat lon} {
    puts "kml.countyfp.$geoid=$name"
    puts "kml.fullcountyfp.$geoid=$name"
}

puts "kml.statefp.label=State FIPS"
puts "kml.countyfp.label=County FIPS"

source dict.states.tcl
source counties.tcl

