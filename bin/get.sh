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
	*)
	    SOURCE=$arg
	    shift
	    ;;
	esac
done


echo "scping $SOURCE from $USER@$IP"

if [ -n "$PEM" ]; then
    scp  -i "${PEM}"  "${USER}@${IP}:$SOURCE" .
else
    scp   "${USER}@${IP}:$SOURCE" .
fi



