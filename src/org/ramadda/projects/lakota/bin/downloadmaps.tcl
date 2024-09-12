proc county {code name}   {
    set url "https://www2.census.gov/geo/tiger/TIGER2023/AREAWATER/tl_2023_${code}_areawater.zip"
#    set url "https://www2.census.gov/geo/tiger/TIGER2023/LINEARWATER/tl_2023_${code}_linearwater.zip"
    puts $code
    regsub -all {  *} $name _ name
    set name [string tolower $name]
    set filename "${name}_areawater.shp.zip"    
    if {![file exists $filename]} {
	catch {exec wget -O  $filename $url} err
    }
}

source sd_codes.tcl

