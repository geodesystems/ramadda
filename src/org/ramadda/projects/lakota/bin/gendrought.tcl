puts "<entries>"

array set N {ens Ensemble noah Noah vic VIC sac SAC mosaic Moasic}
array set hn {{} Current _7  {Past 7 days} _30 {Past 1 month}}
set u "https://www.cpc.ncep.noaa.gov/products/Drought/Figures/smp/"
set ::cnt 0
foreach h {{} _7 _30} {
    foreach p {ens noah vic sac mosaic} {
	incr ::cnt
	set name "$N($p) - $hn($h)"
	set url "${u}${p}${h}.png"
	puts "<entry entryorder=\"$::cnt\" name=\"$name\" type=\"type_image\" url=\"$url\" />"
    }
}

puts "</entries>"
