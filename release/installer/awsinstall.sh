#!/bin/sh

#
#This script installs RAMADDA on an AWS Linux machine
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"
. "${MYDIR}/lib.sh"

export RAMADDA_BASE_DIR="$AWS_BASE_DIR"

install_service() {
    aws_install_service
}


init_env

#mount the volume if needed
if [ ! -d "$RAMADDA_BASE_DIR" ]; then
    aws_do_mount;
fi

echo "RAMADDA Home dir:$RAMADDA_HOME_DIR"
mkdir -p $RAMADDA_HOME_DIR


tmpdir=`dirname $RAMADDA_BASE_DIR`
permissions=$(stat -c %a $tmpdir)
if [ "$permissions" == "700" ]; then
    chmod 755 "$tmpdir"
fi


askYesNo "Do you want to install Java?"  "y"
if [ "$response" == "y" ]; then
    yum install -y java
    sudo /usr/sbin/alternatives --config java
fi


header "Postgres install"
echo "RAMADDA can run with Postgres or it's own built in Derby database"
askYesNo "Do you want to install and use postgres?"  "y"
if [ "$response" == "y" ]; then
    install_postgres
fi

echo "Fixing the localhost name problem"
sed -e 's/HOSTNAME=localhost.localdomain/HOSTNAME=ramadda.localdomain/g' /etc/sysconfig/network> dummy.network
mv dummy.network /etc/sysconfig/network
sed -e 's/127.0.0.1   localhost localhost.localdomain/127.0.0.1 ramadda.localdomain ramadda localhost localhost.localdomain/g' /etc/hosts> dummy.hosts
mv dummy.hosts /etc/hosts

ask_install_ramadda
ask_keystore
generate_install_password

printf "Starting RAMADDA"
service ${SERVICE_NAME} restart

do_finish_message

exit














