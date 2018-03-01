# CropScape wms
puts "<entries>"
set allstates {al ak az ar ca co ct de dc fl ga hi id il in ia ks ky la me mt ne nv nh nj nm ny nc nd oh ok or md ma mi mn ms mo pa ri sc sd tn tx ut vt va wa wv wi wy} 

set states {al az ar ca co ct de fl ga  id il in ia ks ky la me mt ne nv nh nj nm ny nc nd oh ok or md ma mi mn ms mo pa ri sc sd tn tx ut vt va wa wv wi wy} 

if {0} {
foreach state $states {
    set url "http://129.174.131.7/cgi/wms_cdl_${state}.cgi?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetCapabilities"
    puts "\t<entry type=\"type_wms_capabilities\" url=\"$url\" />"
}

} else {
#VegScape wms
    foreach url {
        http://129.174.131.8/cgi-bin/daily_ndvi_2010?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetCapabilities
        http://129.174.131.8/cgi-bin/weekly_ndvi_2012?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetCapabilities
        http://129.174.131.8/cgi-bin/biweekly_ndvi_2012?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetCapabilities
        http://129.174.131.8/cgi-bin/weekly_vci_2012?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetCapabilities
        http://129.174.131.8/cgi-bin/biweekly_vci_2012?SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetCapabilities
        http://129.174.131.8/cgi-bin/weekly_vci_2012?SERVICE=WCS&amp;VERSION=1.0.0&amp;REQUEST=GetCapabilities
    } {
        puts "\t<entry type=\"type_wms_capabilities\" url=\"$url\" />"
    }
}


puts "</entries>"