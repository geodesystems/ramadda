#This downloads groundwater data using the csvget.sh script

set ::scriptDir [file dirname [info script]]

#make the dir
if {![file exists average]} {
    exec mkdir average
}

if {![file exists data]} {
    puts stderr "downloading data"
    exec mkdir data
    cd data
    exec sh ../csvget.sh
    cd ..
}


set ::cnt 0
foreach csv [glob data/*.csv] {
    incr ::cnt
    set name [file tail $csv]
    set file2 "average/$name"
    puts "creating average file: $file2"
    exec sh $::scriptDir/convertgw.sh $csv > "$file2"
    if {$::cnt==1} {
        exec cat $file2 > groundwater_yearly_averages.csv
    } else {
        exec tail -n +2 $file2 >> groundwater_yearly_averages.csv
    }
}

