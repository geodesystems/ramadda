#Sites from:
#http://www.wcc.nrcs.usda.gov/nwcc/yearcount?network=sntl&counttype=statelist

array set ::stateNames {
    UT  {Utah}  
    WA  {Washington}  
    OR  {Oregon}  
    WY  {Wyoming}  
    MT  {Montana}  
    ID  {Idaho}  
    NV  {Nevada}  
    AK  {Alaska}  
    NM  {New Mexico}  
    CO  {Colorado}  
    CA  {California}  
    AZ  {Arizone}  
    SD  {South Dakota}
}

proc cdata {tag s} {
    return "<$tag><!\[CDATA\[$s\]\]></$tag>\n"
}

array set ::months  {January 01 February 02 March 03 April 04 May 05 June 06 July 07 August 08 September 09 October 10 November 11 December 12}


proc snotel {network state site skip start lat lon elev county huc}  {
    set network [string trim $network]

#    if {$state != "CO"} return;

    regexp {(.*)-(.*)} $start match year month

    set date "$year-$::months($month)-01"
    
    regexp  {(.*)\((\d+)\)}  $site match name num
    set name [string trim $name]
    set num [string trim $num]
    set id "$num:$state:$network"


    regexp  {(.*)\((\d+)\)}  $huc match hucName hucId
    set hucName [string trim $hucName]
    set hucId [string trim $hucId]




    set url "http://www.wcc.nrcs.usda.gov/reportGenerator/view_csv/customSingleStationReport/daily/$id|name/-14,0/WTEQ::value,WTEQ::delta,SNWD::value,SNWD::delta,PREC::value,TOBS::value,TMIN::value,TMAX::value"
#    puts "$id - $state -  $network  "
    if {![info exists ::states($state)]} {
        append ::xml "<entry type=\"group\"  name=\"$::stateNames($state)\" id=\"$state\" />\n"
        set ::states($state) $state
    }
    set desc ""
    append desc "<a href=\"http://www.wcc.nrcs.usda.gov/nwcc/site?sitenum=$num\">NRCS Site</a>"
    append ::xml "<entry type=\"type_point_snotel\" name=\"$name\" createdate=\"$date\" parent=\"$state\" latitude=\"$lat\" longitude=\"$lon\" >\n"
    append ::xml [cdata url $url]
    append ::xml [cdata description $desc]
    append ::xml [cdata site_id $id]
    append ::xml [cdata site_number $num]
    append ::xml [cdata state $state]
    append ::xml [cdata network $network]
    append ::xml [cdata huc_name $hucName]
    append ::xml [cdata huc_id $hucId]

    append ::xml "</entry>\n"
}


set ::xml "<entries>\n"
source snotel.sites

append ::xml "</entries>\n"

puts $::xml