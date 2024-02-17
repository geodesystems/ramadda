#!/bin/sh
#for configuring command line aliases for an AWS instance
#This sets up the aliases go$id, put$id, get$id
#usage:
#defineaws.sh  ID IP PEM
#e.g.:
#defineaws.sh geode <some ip> <full path to pem file>


MYDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


ID=$1
IP=$2
PEM=$3


alias go$ID="ssh -i $PEM ec2-user@$IP"
alias put$ID="sh ${MYDIR}/scpgeode.sh ${IP} ${PEM}"
alias get$ID="sh ${MYDIR}/scpgeode2.sh ${IP} ${PEM}"

