#
#This script converts the JSON from realtor.csv to a CSV
#The JSON is from the map search which is:
#https://www.realtor.com/api/v1/hulk?client_id=rdc-x&schema=vesta ...
#
sh ~/bin/csv.sh -json data.home_search.results "list_price,description" -c sqft,list_price -operator "list_price,sqft" "price_per_sqft" / -p "$@"
