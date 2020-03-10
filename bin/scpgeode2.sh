#!/bin/sh
export dest_ip=$1
echo "scping $2 to $dest_ip"
scp -i ${GEODESYSTEMS_PEM}  ec2-user@${dest_ip}:$2 .


