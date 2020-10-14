#!/bin/sh
export dest_ip=$1
echo "scping $2 to $dest_ip"
scp -r -i ${GEODESYSTEMS_PEM} $2 ec2-user@${dest_ip}:


