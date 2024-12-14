#!/bin/sh
#for configuring command line aliases for an AWS instance
#This sets up the aliases go$id, put$id, get$id, update$id, devupdate$id
#usage:
#sh defineenv.sh  <some id> <some IP> -pem <pem file> -user <user>

#e.g.: access with the pem file. defaults to user ec2-user
#sh defineenv.sh test <some ip> -pem <full path to pem file>

#e.g.: password access. user=ubuntu
#sh defineenv.sh test <some ip> -user ubuntu

#
#generated commands:
#
#using:
#defineenv.sh test <some ip> -pem <pem file>
#will give aliases:

#ssh to machine:
#gotest 

#scp the file to machine:
#puttest  <some file>  

#scp the file from machine:
#gettest  <some file>  

#this will run the RAMADDA update script.
#This assumes the ramaddainstaller directory is in the home directory
#updatetest

#This updates from the RAMADDA development release
#devupdatetest  

MYDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


unset RAMADDAINSTALL
unset PEM
unset USER

ID=$1
IP=$2
USER=ec2-user
RAMADDAINSTALL=/mnt/ramadda/ramaddainstall


shift
shift

while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -pem)
	    shift
	    export PEM=$1
	    shift
            ;;
        -user)
	    shift
	    export USER="$1"
	    shift
	    ;;
	-dir)
	    shift
	    export RAMADDAINSTALL="$1"
	    shift
	    ;;
	*)
	    echo "Unknown argument:$arg"
	    usage
	    exit 1
	    ;;
	esac
done




alias ip_$ID="echo $IP"

if [ -n "$PEM" ]; then
    alias go$ID="ssh -i $PEM ${USER}@$IP"
    alias put$ID="sh ${MYDIR}/put.sh ${IP} -pem ${PEM} -user ${USER}"
    alias get$ID="sh ${MYDIR}/get.sh ${IP} -pem ${PEM} -user ${USER}"
    eval "update$ID() {
       echo \"updating $ID ${USER}@${IP} install dir: ${RAMADDAINSTALL}\"
       ssh -i ${PEM} ${USER}@${IP} \"sudo bash ramaddainstaller/update.sh -dir ${RAMADDAINSTALL}\"
    }"
    eval "devupdate$ID() {
       echo \"updating $ID ${USER}@${IP} install dir: ${RAMADDAINSTALL}\"
       ssh -i ${PEM} ${USER}@${IP} \"sudo bash ramaddainstaller/update.sh -dev -dir ${RAMADDAINSTALL}\"
    }"    
else
    alias go$ID="ssh  ${USER}@$IP"
    alias put$ID="sh ${MYDIR}/put.sh ${IP} -user ${USER}"
    alias get$ID="sh ${MYDIR}/get.sh  -user ${USER}"
    eval "update$ID() {
           echo \"updating $ID ${USER}@${IP} install dir: ${RAMADDAINSTALL}\"
	   ssh ${USER}@${IP} \"sudo bash ramaddainstaller/update.sh -dir ${RAMADDAINSTALL}\"
    }"
    eval "devupdate$ID() {
           echo \"updating $ID ${USER}@${IP} install dir: ${RAMADDAINSTALL}\"
	   ssh ${USER}@${IP} \"sudo bash ramaddainstaller/update.sh -dev -dir ${RAMADDAINSTALL}\"
    }"

fi

