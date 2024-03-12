export OVERWRITE=0

makedir() {
    if ! test -e "$1" ; then
	mkdir "$1";
    fi
}
download() {
    message="$1";
    filename="$2";
    url="$3"
    length="$4"
    if [ "$OVERWRITE" -eq 0 ] && [ -e "$filename" ] && [ -s "$filename" ] && [ $(stat -f%z "$filename") -eq "$length" ]; then
	echo "Already exists: $filename";
	return;
    fi
    if [ "$OVERWRITE" -eq 0 ] && [ -e "$filename" ] && [ -s "$filename" ] && [ "$length" -eq "-1" ]; then
	echo "Already exists: $filename";
	return;	
    fi

    echo "$message";
    touch "$filename.tmp"
    ${DOWNLOAD_COMMAND}  ${downloadargs} "$filename.tmp" "$url"
    if [[ $? != 0 ]] ; then
	echo "download failed url:$url"
	exit $?
    fi
    mv "$filename.tmp" "$filename" 
}

#
# parse the args
#
while [ "$#" -gt 0 ]; do
    echo "ARG:$1"
    case "$1" in
        -overwrite)
            shift
            export OVERWRITE=1
            ;;
        *)
            echo "Unknown argument $1"
            exit
            ;;
    esac
    shift
done


