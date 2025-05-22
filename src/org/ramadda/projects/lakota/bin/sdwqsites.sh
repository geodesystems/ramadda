


sh /Users/jeffmc/bin/seesv.sh -json features attributes -p sites1.json > sites1.csv
sh /Users/jeffmc/bin/seesv.sh -json features attributes -p sites2.json > sites2.csv
sh /Users/jeffmc/bin/seesv.sh -json features attributes -p sites3.json > sites3.csv
sh /Users/jeffmc/bin/seesv.sh -json features attributes -p sites4.json > sites4.csv
sh /Users/jeffmc/bin/seesv.sh -json features attributes -p sites5.json > sites5.csv
sh /Users/jeffmc/bin/seesv.sh -p sites*.csv > tmp.csv
sh /Users/jeffmc/bin/seesv.sh -columns "stationid,waterbodyname,type,latitude,longitude,au_id" -p tmp.csv > wqsites.csv
exit



wget -O sites1.json "https://arcgis.sd.gov/arcgis/rest/services/DENR/NR92_WQMAPPublic/MapServer/0/query?f=json&xgeometry={%22spatialReference%22%3A{%22latestWkid%22%3A3857%2C%22wkid%22%3A102100}%2C%22xmin%22%3A-11427338.989946868%2C%22ymin%22%3A5447141.461679736%2C%22xmax%22%3A-11423670.012589183%2C%22ymax%22%3A5450810.439037424}&outFields=*&spatialRel=esriSpatialRelIntersects&where=1%3D1&geometryType=esriGeometryEnvelope&inSR=102100&outSR=102100"

wget -O sites2.json "https://arcgis.sd.gov/arcgis/rest/services/DENR/NR92_WQMAPPublic/MapServer/0/query?f=json&xgeometry={%22spatialReference%22%3A{%22latestWkid%22%3A3857%2C%22wkid%22%3A102100}%2C%22xmin%22%3A-11427338.989946868%2C%22ymin%22%3A5447141.461679736%2C%22xmax%22%3A-11423670.012589183%2C%22ymax%22%3A5450810.439037424}&outFields=*&spatialRel=esriSpatialRelIntersects&where=1%3D1&geometryType=esriGeometryEnvelope&inSR=102100&outSR=102100&resultOffset=1000"


wget -O sites3.json "https://arcgis.sd.gov/arcgis/rest/services/DENR/NR92_WQMAPPublic/MapServer/0/query?f=json&xgeometry={%22spatialReference%22%3A{%22latestWkid%22%3A3857%2C%22wkid%22%3A102100}%2C%22xmin%22%3A-11427338.989946868%2C%22ymin%22%3A5447141.461679736%2C%22xmax%22%3A-11423670.012589183%2C%22ymax%22%3A5450810.439037424}&outFields=*&spatialRel=esriSpatialRelIntersects&where=1%3D1&geometryType=esriGeometryEnvelope&inSR=102100&outSR=102100&resultOffset=2000"

wget -O sites4.json "https://arcgis.sd.gov/arcgis/rest/services/DENR/NR92_WQMAPPublic/MapServer/0/query?f=json&xgeometry={%22spatialReference%22%3A{%22latestWkid%22%3A3857%2C%22wkid%22%3A102100}%2C%22xmin%22%3A-11427338.989946868%2C%22ymin%22%3A5447141.461679736%2C%22xmax%22%3A-11423670.012589183%2C%22ymax%22%3A5450810.439037424}&outFields=*&spatialRel=esriSpatialRelIntersects&where=1%3D1&geometryType=esriGeometryEnvelope&inSR=102100&outSR=102100&resultOffset=3000"

wget -O sites5.json "https://arcgis.sd.gov/arcgis/rest/services/DENR/NR92_WQMAPPublic/MapServer/0/query?f=json&xgeometry={%22spatialReference%22%3A{%22latestWkid%22%3A3857%2C%22wkid%22%3A102100}%2C%22xmin%22%3A-11427338.989946868%2C%22ymin%22%3A5447141.461679736%2C%22xmax%22%3A-11423670.012589183%2C%22ymax%22%3A5450810.439037424}&outFields=*&spatialRel=esriSpatialRelIntersects&where=1%3D1&geometryType=esriGeometryEnvelope&inSR=102100&outSR=102100&resultOffset=4000"








