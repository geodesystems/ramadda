#!/bin/sh
mydir=`dirname $0`


#This is the script I use to build and restart the main RAMADDA server on AWS
#You need to set the following in your environment to point to the geode systems 
#server and the .pem file (for logging in)
#export GEODESYSTEMS_IP=52.88.245.74
#export GEODESYSTEMS_PEM=/path/to/geodesystems.pem


#I run this script with the alias buildgeode:
#alias buildgeode="sh ${RAMADDA_BIN}/makerelease.sh ${GEODESYSTEMS_IP}"

#Location of the PEM file to log in to the AWS server
PEM=${GEODESYSTEMS_PEM}

##TODO: process args for dest ip and pem file
ipaddress=$1
user=ec2-user

target="release"
if [  "$2" ]; then
    target="$2"
fi
ssh -tq -i  ${PEM}  ${user}@${ipaddress} "sudo sh /mnt/ramadda/source/ramadda/bin/buildandinstall.sh ${target}"

tput bel
tput bel

