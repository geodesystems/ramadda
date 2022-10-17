#!/bin/sh
export dest_ip=$1
echo "scping $2 to $dest_ip"
if [ -z "$3" ]; then
    scp -r -i ${GEODESYSTEMS_PEM} $2 ec2-user@${dest_ip}:
else
    scp -r -i ${GEODESYSTEMS_PEM} $2 ec2-user@${dest_ip}:$3
fi


