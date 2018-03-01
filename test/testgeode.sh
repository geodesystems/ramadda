declare -a ids=("6eb95a42-b146-4352-9a97-ce677f8373ae" "ebbe1ab5-423a-4488-93fb-28bdcc792a17" "2e485e95-eb29-44fc-8987-76e6ac74365a" "59324c53-f8ab-4d38-a027-3aa8d7fc0513" )

declare -a outputs=("kml"
"rss.full"
"atom"
"ical"
"json"
"default.csv"
"dif.xml"
"dif.text"
"thredds.catalog"
"default.html"
"html.info"
"html.table"
"html.treeview"
"map.map"
"map.gemap"
"calendar.calendar"
"default.timeline"
"graph.graph"
)

for id in "${ids[@]}"
do
    echo "entry: ${id}"
    for output in "${outputs[@]}"
    do
        url="http://geodesystems.com/repository/entry/show?entryid=${id}&output=${output}"
        curl -s -S  -o test.out  ${url}  
        if (($? > 0)); then
            echo "FAILED:  ${url}"
            exit 1
        fi
#        echo "output: $output"
#        egrep "\\$\{" test.out | grep -v latitude
    done
done

