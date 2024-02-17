#!/bin/sh
IP=$1
PEM=$2
SOURCE=$3
echo "getting $SOURCE from $IP"
scp -i ${PEM}  "ec2-user@${IP}:$SOURCE" .


