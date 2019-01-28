#!/bin/sh


#This is the script I use to build and restart the main RAMADDA server on AWS
#I run this script with the alias buildgeode:

#export geodesystems_ip=52.88.245.74
#alias buildgeode="sh ~/work/bin/makerelease.sh ${geodesystems_ip}"

#All you need to do is change the location of the PEM file

#Location of the PEM file to log in to the AWS server
PEM=/Users/jeffmc/work/amazon/geodesystems.pem


myDir=`dirname $0`
ipAddress=$1
user=ec2-user

##TODO: process args for dest ip and pem file

ssh -tq -i  ${PEM}  ${user}@${ipAddress} "sudo sh /home/ec2-user/ramadda/buildandinstall.sh"



