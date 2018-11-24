puts "<entries>"
proc state {url name} {
    regsub -all { } {_} $name _name
    set _house "State_Senate";
    regsub -all $_house _ { } house
    set descUrl "<a href=\"https://ballotpedia.org/${_name}_${_house}\">ballotpedia.org</a>"
    set desc "<description><!\[CDATA\[\n+note\nMore information at ${descUrl}\n-note\n\]\]></description>"
    puts "<entry type=\"geo_shapefile\" download=\"true\" url=\"$url\" name=\"$name $house Map\">"
    puts $desc
    puts "</entry>"
}


state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_01_sldu_500k.zip {Alabama}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_02_sldu_500k.zip {Alaska}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_04_sldu_500k.zip {Arizona}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_05_sldu_500k.zip {Arkansas}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_06_sldu_500k.zip {California}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_08_sldu_500k.zip {Colorado}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_09_sldu_500k.zip {Connecticut}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_10_sldu_500k.zip {Delaware}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_11_sldu_500k.zip {District of Columbia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_12_sldu_500k.zip {Florida}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_13_sldu_500k.zip {Georgia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_15_sldu_500k.zip {Hawaii}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_16_sldu_500k.zip {Idaho}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_17_sldu_500k.zip {Illinois}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_18_sldu_500k.zip {Indiana}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_19_sldu_500k.zip {Iowa}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_20_sldu_500k.zip {Kansas}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_21_sldu_500k.zip {Kentucky}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_22_sldu_500k.zip {Louisiana}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_23_sldu_500k.zip {Maine}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_24_sldu_500k.zip {Maryland}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_25_sldu_500k.zip {Massachusetts}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_26_sldu_500k.zip {Michigan}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_27_sldu_500k.zip {Minnesota}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_28_sldu_500k.zip {Mississippi}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_29_sldu_500k.zip {Missouri}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_30_sldu_500k.zip {Montana}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_31_sldu_500k.zip {Nebraska}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_32_sldu_500k.zip {Nevada}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_33_sldu_500k.zip {New Hampshire}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_34_sldu_500k.zip {New Jersey}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_35_sldu_500k.zip {New Mexico}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_36_sldu_500k.zip {New York}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_37_sldu_500k.zip {North Carolina}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_38_sldu_500k.zip {North Dakota}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_39_sldu_500k.zip {Ohio}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_40_sldu_500k.zip {Oklahoma}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_41_sldu_500k.zip {Oregon}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_42_sldu_500k.zip {Pennsylvania}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_44_sldu_500k.zip {Rhode Island}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_45_sldu_500k.zip {South Carolina}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_46_sldu_500k.zip {South Dakota}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_47_sldu_500k.zip {Tennessee}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_48_sldu_500k.zip {Texas}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_49_sldu_500k.zip {Utah}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_50_sldu_500k.zip {Vermont}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_51_sldu_500k.zip {Virginia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_53_sldu_500k.zip {Washington}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_54_sldu_500k.zip {West Virginia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_55_sldu_500k.zip {Wisconsin}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_56_sldu_500k.zip {Wyoming}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_72_sldu_500k.zip {Puerto Rico}

puts "</entries>"
