#!/bin/sh

#
#basic shared initialization
#


INSTALLER_DIR="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"


export OS_REDHAT="redhat"
export OS_AMAZON="amazon_linux"

##For now only amazon linux is supported 
export os=$OS_AMAZON

export RAMADDA_DOWNLOAD="https://geodesystems.com/repository/release/latest/ramaddaserver.zip"
export PARENT_DIR=`dirname $INSTALLER_DIR`
export YUM_ARG=""
export USER_DIR=$PARENT_DIR
export BASE_DIR=/mnt/ramadda
export RAMADDA_HOME_DIR=$BASE_DIR/repository
export MOUNT_DIR=""

export promptUser=1



usage() {
    echo "installer.sh -os redhat -y (assume yes installer) -help "
    exit
}

header() {
    local msg="$1"
    printf "\n*** ${msg} ***\n";
}


yumInstall() {
    local target="$1"
    if [ "$YUM_ARG" == "" ]; then
	yum install ${target}
    else 
	yum install ${YUM_ARG} ${target}
    fi
}

askYesNo() {
    local msg="$1"
    local dflt="$2"

    if [ $promptUser == 0 ]; then
	response="$dflt";
	return;
    fi

    read -p "${msg}?  [y|A(all)|n]: " response
    if [ "$response" == "A" ]; then
	promptUser=0;
	response="$dflt";
	return;
    fi

    case $response in y|Y) 
            response='y'
            ;;  
	"")
	    response="$dflt";
	    ;;

        *) response='n'
            ;;
    esac

    if [ "$response" == "" ]; then
	response="$dflt";
    fi
    if [ "$response" == "" ]; then
	response="n";
    fi

}

ask() {
    local msg="$1";
    local dflt="$2";
    local extra="$3"
    if [ $promptUser == 0 ]; then
	response="$dflt";
        return;
    fi

    if [ "$extra" != "" ]; then
        printf "\n# $extra\n"
    fi

    read -p "${msg} " response;

    if [ "$response" == "" ]; then
	response="$dflt";
    fi
}


#
# parse the args
#
while [ $# != 0 ]
do
    case $1 in 
	-os)
	    shift
	    os=$1;
	    ;;
	-help)
	    usage
	    ;;
	-y)
	    promptUser=0
	    YUM_ARG=--assumeyes
	    ;;
	*)
	    echo "Unknown argument $1"
	    usage
	    ;;
    esac
    shift
done



if [ "$os" == "${OS_REDHAT}" ]; then
    export PG_SERVICE=postgresql-server
    export PG_INSTALL=http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-redhat93-9.3-1.noarch.rpm
else
    export PG_SERVICE=postgresql.service
    export PG_INSTALL=postgresql-server
fi

export PG_DIR=/var/lib/pgsql
export PG_DATA_DIR=${PG_DIR}/data
export PG_REAL_DIR="${BASE_DIR}/pgsql"

installPostgres() {
    echo "Installing postgresql13 with"
    printf "\tamazon-linux-extras install postgresql13 vim epel\n\tyum install -y  ${PG_INSTALL}\n"
    amazon-linux-extras install postgresql13 vim epel > /dev/null
    yum install -y  ${PG_INSTALL} > /dev/null
    if  [ ! -d ${PG_DIR} ] ; then
	echo "setting up postgres"
	postgresql-setup --initdb --unit postgresql
    fi
    echo "adding postgres service"
    systemctl enable ${PG_SERVICE}
    systemctl start ${PG_SERVICE}
#	chkconfig  on ${PG_SERVICE}
#	service ${PG_SERVICE} start


    ##if /var/lib/pgsql exists and it isn't a link
    if  [ -d ${PG_DIR} ]; then 
	if [ ! -h ${PG_DIR} ]; then 
	    if  [  -d ${PG_REAL_DIR} ] ; then
		echo "Looks like ${PG_REAL_DIR} already exists. Moving ${PG_DIR} to ${PG_DIR}.bak"
		mv ${PG_DIR} ${PG_DIR}.bak
	    else
		echo "Moving ${PG_DIR} to $PG_REAL_DIR"
		mv  ${PG_DIR} $PG_REAL_DIR
	    fi
	    echo "linking $PG_DIR to $PG_REAL_DIR"
	    ln  -s -f  $PG_REAL_DIR ${PG_DIR}
	    chown -R postgres ${PG_DIR}
	    chown -R postgres ${PG_REAL_DIR}
	else
	    echo "Looks like ${PG_DIR} is already linked to ${PG_REAL_DIR}"
	fi
    else
	echo "Warning: ${PG_DIR} does not exist"	
    fi



    PG_PASSWORD="password$RANDOM-$RANDOM"
    PG_USER="ramadda"

    if [ ! -f ${PG_DATA_DIR}/pg_hba.conf.bak ]; then
	#If we haven't updated pg_hba then 
	echo "adding $PG_USER to  ${PG_DATA_DIR}/pg_hba.conf "
        cp ${PG_DATA_DIR}/pg_hba.conf ${PG_DATA_DIR}/pg_hba.conf.bak
	postgresAuth="
#	
#written out by the RAMADDA installer
#
host repository ${PG_USER} 127.0.0.1/32  password
#For now add this as I can't get the password to work
local   repository      ${PG_USER}                                  trust
local   all             all                                     peer
host    all             all             127.0.0.1/32            ident
host    all             all             ::1/128                 ident
"


	printf "${postgresAuth}" > ${PG_DATA_DIR}/pg_hba.conf
	systemctl reload postgresql.service
	service ${PG_SERVICE} reload
    fi

    systemctl reload postgresql.service
#    service ${PG_SERVICE} reload

    printf "create database repository;\ncreate user ramadda;\nalter user ramadda with password '${PG_PASSWORD}';\ngrant all privileges on database repository to ramadda;\n" > /tmp/postgres.sql
    chmod 644 /tmp/postgres.sql
    echo "Creating repository database, adding ramadda user and setting privileges and password"
    su -c "psql -f /tmp/postgres.sql"  - postgres > /dev/null
    rm -f ${INSTALLER_DIR}/postgres.sql
    echo "writing properties to ${RAMADDA_HOME_DIR}/db.properties"
    printf "ramadda.db=postgres\nramadda.db.postgres.user=ramadda\nramadda.db.postgres.password=${PG_PASSWORD}"  > ${RAMADDA_HOME_DIR}/db.properties

    
}
