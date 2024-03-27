#!/bin/sh
#the SEESV environment variable needs to point to the directory holding the seesv.sh script in RAMADDA's SeeSV  release
#


merge() {
sh ${SEESV}/seesv.sh "-delimiter" "?" "-skiplines" "1" "-set" "0" "0" "number" "-set" "1" "0" "Date Time" "-set" "2" "0" "Temperature" "-notcolumns" "0,3-10" "-indateformats" "MM/dd/yy hh:mm:ss a;MM/dd/yyyy HH:mm" "GMT-4" "-outdateformat" "iso8601" "GMT" "-convertdate" "date_time" "-outdateformat" "yyyy-MM-dd HH:mm Z" "UTC" "-indateformat" "iso8601" "GMT" "-extractdate" "date_time" "year" "-extractdate" "date_time" "hours_in_year" "-notcolumns" "date_time" "-lastcolumns" "0"  -print  $1
}




echo "Year,Hours in year,Temperature" > tmp.csv
for arg in "$@"; do
    merge $arg| tail -n +2 >>tmp.csv
done
sh ${SEESV}/seesv.sh \
   -makefields year temperature hours_in_year "" \
   -sortby hours_in_year up "" \
   -formatdateoffset hours_in_year hours_in_year \
   -firstcolumns month_day_hour\
   -addheader "month_day_hour.type string hours_in_year.type int default.type double" \
   -p tmp.csv 
