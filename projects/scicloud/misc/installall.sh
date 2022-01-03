#!/bin/sh
dir=`dirname $0`
bindir=/home/ec2-user/bin
mkdir -p ${bindir}
sudo yum-config-manager --enable epel



