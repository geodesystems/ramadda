#!/bin/sh

#
#This script installs RAMADDA on an AWS Linux machine
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"
. "${MYDIR}/lib.sh"


install_service() {
    aws_install_service
}

install_postgres() {
    aws_install_postgres
}

install_java() {
    yum install -y java
    sudo /usr/sbin/alternatives --config java
}

install_step1() {
    echo "Fixing the localhost name problem"
    sed -e 's/HOSTNAME=localhost.localdomain/HOSTNAME=ramadda.localdomain/g' /etc/sysconfig/network> dummy.network
    mv dummy.network /etc/sysconfig/network
    sed -e 's/127.0.0.1   localhost localhost.localdomain/127.0.0.1 ramadda.localdomain ramadda localhost localhost.localdomain/g' /etc/hosts> dummy.hosts
    mv dummy.hosts /etc/hosts
}



start_service() {
    printf "Starting RAMADDA"
    service ${SERVICE_NAME} restart
}


do_main_install


exit














