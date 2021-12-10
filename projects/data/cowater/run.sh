

csv="java -Xmx2048m org.ramadda.util.text.CsvUtil"

${csv}  -pattern station_type "Stream Gage" \
       -set abbrev 0 site_id \
       -case station_name_display proper \
       -template "<entries>" file:cowater.txt "" "</entries>"  stations.csv 






