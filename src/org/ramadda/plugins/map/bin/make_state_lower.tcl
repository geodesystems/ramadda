puts "<entries>"
proc state {url name} {
    set name [string trim $name]
    regsub -all { } $name {_}  _name
    set _house "House_of_Representatives";
    if {$name == "California"}  {
        set _house "State_Assembly"
    } elseif {$name == "Maryland"}  {
        set _house "House_of_Delegates"
    } elseif {$name == "Virginia"}  {
        set _house "House_of_Delegates"
    } elseif {$name == "Nevada"}  {
        set _house "State_Assembly"
    } elseif {$name == "Wisconsin"}  {
        set _house "State_Assembly"
    } elseif {$name == "New Jersey"}  {
        set _house "General_Assembly"
    } elseif {$name == "New York"}  {
        set _house "State_Assembly"
    } elseif {$name == "California"}  {
        set _house "State_Assembly"
    }
    regsub -all _ $_house { } house
    set descUrl "<a href=\"https://ballotpedia.org/${_name}_${_house}\">ballotpedia.org</a>"
    set desc "<description><!\[CDATA\[\n+note\nMore information at ${descUrl}\n-note\n\]\]></description>"
    puts "<entry type=\"geo_shapefile\" download=\"true\" url=\"$url\" name=\"$name $house Map\">"
    puts $desc
    puts "</entry>"
}

state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_01_sldl_500k.zip {Alabama}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_02_sldl_500k.zip {Alaska}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_04_sldl_500k.zip {Arizona}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_05_sldl_500k.zip {Arkansas}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_06_sldl_500k.zip {California}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_08_sldl_500k.zip {Colorado}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_09_sldl_500k.zip {Connecticut}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_10_sldl_500k.zip {Delaware}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_12_sldl_500k.zip {Florida}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_13_sldl_500k.zip {Georgia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_15_sldl_500k.zip {Hawaii}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_16_sldl_500k.zip {Idaho}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_17_sldl_500k.zip {Illinois}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_18_sldl_500k.zip {Indiana}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_19_sldl_500k.zip {Iowa}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_20_sldl_500k.zip {Kansas}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_21_sldl_500k.zip {Kentucky}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_22_sldl_500k.zip {Louisiana}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_23_sldl_500k.zip {Maine}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_24_sldl_500k.zip {Maryland}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_25_sldl_500k.zip {Massachusetts}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_26_sldl_500k.zip {Michigan}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_27_sldl_500k.zip {Minnesota}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_28_sldl_500k.zip {Mississippi}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_29_sldl_500k.zip {Missouri}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_30_sldl_500k.zip {Montana}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_32_sldl_500k.zip {Nevada}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_33_sldl_500k.zip {New Hampshire}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_34_sldl_500k.zip {New Jersey}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_35_sldl_500k.zip {New Mexico}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_36_sldl_500k.zip {New York}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_37_sldl_500k.zip {North Carolina}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_38_sldl_500k.zip {North Dakota}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_39_sldl_500k.zip {Ohio}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_40_sldl_500k.zip {Oklahoma}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_41_sldl_500k.zip {Oregon}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_42_sldl_500k.zip {Pennsylvania}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_44_sldl_500k.zip {Rhode Island}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_45_sldl_500k.zip {South Carolina}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_46_sldl_500k.zip {South Dakota}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_47_sldl_500k.zip {Tennessee}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_48_sldl_500k.zip {Texas}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_49_sldl_500k.zip {Utah}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_50_sldl_500k.zip {Vermont}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_51_sldl_500k.zip {Virginia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_53_sldl_500k.zip {Washington}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_54_sldl_500k.zip {West Virginia}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_55_sldl_500k.zip {Wisconsin}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_56_sldl_500k.zip {Wyoming}
state https://www2.census.gov/geo/tiger/GENZ2017/shp/cb_2017_72_sldl_500k.zip {Puerto Rico}

puts "</entries>"