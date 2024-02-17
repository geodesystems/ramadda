#!/bin/sh
#usage: scpgeode.sh <DEST IP> <PEM FILE> <SOURCE FILE> <DEST DIR - optional>
IP=$1
PEM=$2
SOURCE=$3
DEST=$4
echo "scping $3 to $IP"
if [ -z "$DEST" ]; then
    scp -r -i $PEM "$SOURCE" ec2-user@${IP}:
else
    scp -r -i $PEM "$SOURCE" ec2-user@${IP}:$DEST
fi


