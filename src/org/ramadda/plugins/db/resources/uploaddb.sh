

#e.g.:
#uploadhost="https://localhost:8430"

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




