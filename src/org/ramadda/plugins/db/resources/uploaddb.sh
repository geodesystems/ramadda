
#usage:
#uploaddb.sh https://ramadda.org uploadkey entry_id file.txt
#where file.txt is, e.g.:
#temp=5&rh=0.4
#temp=7&rh=0.6
#...

uploadhost="${1}"
uploadkey="${2}"
uploadentry="${3}"

upload() {
    args="${1}"
    url="${uploadhost}/db/upload?entryid=${uploadentry}&key=${uploadkey}&${args}"
    curl -k "$url"
}

cat "${4}" | while read line 
do
    upload "${line}"
done




