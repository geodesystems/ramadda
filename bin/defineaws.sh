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
USER=ec2-user
RAMADDAINSTALL=/mnt/ramadda/ramaddainstall
if [ $# -ge 4 ]; then
    USER=$4
fi


if [ $# -ge 5 ]; then
    RAMADDAINSTALL=$5
fi


alias go$ID="ssh -i $PEM ${USER}@$IP"
alias put$ID="sh ${MYDIR}/scpaws.sh ${IP} ${PEM}"
alias get$ID="sh ${MYDIR}/scpaws2.sh ${IP} ${PEM}"
alias update$ID='ssh -i ${PEM} ${USER}@${IP} "sudo bash ramaddainstaller/awsupdate.sh -dir ${RAMADDAINSTALL}"'

