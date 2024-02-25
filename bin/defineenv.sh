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
	    export RAMADDAINSTALL=/mnt/ramadda/ramaddainstall
	    shift
	    ;;
	*)
	    echo "Unknown argument:$arg"
	    usage
	    exit 1
	    ;;
	esac
done




if [ -n "$PEM" ]; then
    alias go$ID="ssh -i $PEM ${USER}@$IP"
    alias put$ID="sh ${MYDIR}/put.sh ${IP} -pem ${PEM} -user ${USER}"
    alias get$ID="sh ${MYDIR}/get.sh ${IP} -pem ${PEM} -user ${USER}"
    alias update$ID='ssh -i ${PEM} ${USER}@${IP} "sudo bash ramaddainstaller/awsupdate.sh -dir ${RAMADDAINSTALL}"'
else
    alias go$ID="ssh  ${USER}@$IP"
    alias put$ID="sh ${MYDIR}/put.sh ${IP} -user ${USER}"
    alias get$ID="sh ${MYDIR}/get.sh  -user ${USER}"
    alias update$ID='ssh ${USER}@${IP} "sudo bash ramaddainstaller/linuxupdate.sh -dir ${RAMADDAINSTALL}"'
fi

