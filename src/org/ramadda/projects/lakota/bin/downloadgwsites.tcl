set states {AL AK AZ AR CA CO CT DE FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY }

package require json

set url1 "https://www.usgs.gov/apps/ngwmn/state_api/wl/states"


foreach state $states {
    set csv "$state.csv"
    set zip "$state.zip"
    if {![file exists $zip]} {
	catch {exec rm gw.zip}
	catch {exec rm WATERLEVEL.csv}
	set payload payload/payload.$state.txt
	if {![file exists $payload]} {
	    puts "downloading payload for  $state"
	    catch {exec wget -O $payload $url1/$state} err
#	    puts "ERROR: $err"
	}
	set json [read [open $payload r]]
	set j [json::json2dict $json]
	set url1   [dict get  $j download_url]
	puts "downloading zip $zip"
	catch {exec wget -O $zip $url1} err
    }
    exec unzip $zip
    exec mv WATERLEVEL.csv $state.csv
    puts "processing  $csv"
    exec sh $::env(SEESV) -unique "0,1" exact -c "0,1" -skiplines 1 \
	-template "#$state groundwater sites\n" "\${0}:\${1}" "\n" ""  $csv > sites/${state}.txt
}
