if {![file exists average]} {
    exec mkdir average
}

if {![file exists data]} {
    exec mkdir data
}

set csv /Users/jeffmc/bin/seesv.sh
exec sh $csv -columns "id,name" -unique name exact -tcl site Search_Results.csv > sites.tcl
set ::cnt 0

proc site {id name} {
    set file "data/${name}.csv"
    if {![file exists $file]} {
	puts "fetching $file"
	catch {exec wget -O $file https://ramadda.org/repository/entry/show?entryid=$id&output=points.product&getdata=Get%20Data&product=points.csv&addlatlon=true}
    }
    incr ::cnt
    set file2 "average/${name}_average.csv"
    exec sh convertgw.sh $file > "$file2"
    puts "$file2"
    if {$::cnt==1} {
	exec cat $file2 > all.csv
    } else {
	exec tail -n +2 $file2 >> all.csv
    }
}

source sites.tcl


