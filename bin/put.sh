#!/bin/sh
#usage: put.sh <DEST IP> <SOURCE FILE> -pem <pem file - optional> -dest <dest dir - optional>
IP=$1
USER=ec2-user
shift

while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -pem)
	    shift
	    export PEM=$1
	    shift
            ;;
        -user)
	    shift
	    export USER="$1"
	    shift
	    ;;
	-dest)
	    shift
	    export DEST="$1"
	    shift
	    ;;
	*)
	    SOURCE=$arg
	    shift
	    ;;
	esac
done


echo "scping $SOURCE to $USER@$IP"

if [ -n "$PEM" ]; then
    if [ -z "$DEST" ]; then
	scp -r -i $PEM "$SOURCE" ${USER}@${IP}:
    else
	scp -r -i $PEM "$SOURCE" ${USER}@${IP}:$DEST
    fi
else
    if [ -z "$DEST" ]; then
	scp -r  "$SOURCE" ${USER}@${IP}:
    else
	scp -r  "$SOURCE" ${USER}@${IP}:$DEST
    fi

fi



