#!/bin/sh

#
#This script installs RAMADDA on an AWS Linux machine
#

export MYDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${MYDIR}/lib.sh"

export BASE_DIR=/mnt/ramadda
#This comes after setting BASE_DIR
init_env

#mount the volume if neede
if [ ! -d "$BASE_DIR" ]; then
    aws_do_mount;
else
    echo "$BASE_DIR already mounted"
fi

echo "RAMADDA Home dir:$RAMADDA_HOME_DIR"
mkdir -p $RAMADDA_HOME_DIR


tmpdir=`dirname $BASE_DIR`
permissions=$(stat -c %a $tmpdir)
if [ "$permissions" == "700" ]; then
    chmod 755 "$tmpdir"
fi


install_service() {
    aws_install_service
}


#java
echo "Installing Java"
yum install -y java
sudo /usr/sbin/alternatives --config java
sudo /usr/sbin/alternatives --config javac


askYesNo "Install postgres"  "y"
if [ "$response" == "y" ]; then
    installPostgres
fi

echo "Fixing the localhost name problem"
sed -e 's/HOSTNAME=localhost.localdomain/HOSTNAME=ramadda.localdomain/g' /etc/sysconfig/network> dummy.network
mv dummy.network /etc/sysconfig/network
sed -e 's/127.0.0.1   localhost localhost.localdomain/127.0.0.1 ramadda.localdomain ramadda localhost localhost.localdomain/g' /etc/hosts> dummy.hosts
mv dummy.hosts /etc/hosts

ask_install_ramadda
ask_ip
ask_keystore
generate_install_password
service ${SERVICE_NAME} restart

header "Installation complete";
printf "RAMADDA is installed. \n\tRAMADDA home directory: ${RAMADDA_HOME_DIR}\n\tPostgres directory: ${PG_REAL_DIR}\n\tLog file: ${RUNTIME_DIR}/ramadda.log\n"
printf "Finish the configuration at https://<your IP>/repository\n"
printf "The installation password is ${install_password}\n"

exit














