#
#This takes as a command line argument the CSV file of entry information
#of the groundwater sites from https://sicangudata.org/repository/search/type/type_usgs_ngwm
#and creates a groundwwater_yearly_averages.csv file
#

set ::scriptDir [file dirname [info script]]

if {![file exists average]} {
    exec mkdir average
}

if {![file exists data]} {
    exec mkdir data
}

#pull out the id and name of the sites
exec sh $env(SEESV) -columns "id,name" -unique name exact -tcl site [lindex $argv  0] > sites.tcl
set ::cnt 0

proc site {id name} {
    set file "data/${name}.csv"
    if {![file exists $file]} {
	puts "fetching $file"
	catch {exec wget -O $file https://sicangudata.org/repository/entry/show?entryid=$id&output=points.product&getdata=Get%20Data&product=points.csv&addlatlon=true}
    }
    incr ::cnt
    set file2 "average/${name}_average.csv"
#Convert the file
    exec sh $::scriptDir/convertgw.sh $file > "$file2"
    puts "$file2"
    if {$::cnt==1} {
	exec cat $file2 > groundwwater_yearly_averages.csv
    } else {
	exec tail -n +2 $file2 >> groundwwater_yearly_averages.csv
    }
}

source sites.tcl


