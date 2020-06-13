#!/bin/sh

#
#This script installs some base packages, Postgres and then RAMADDA
#


OS_REDHAT="redhat"
OS_AMAZON="amazon_linux"
os=$OS_AMAZON

serviceName="ramadda"
installerDir=`dirname $0`
parentDir=`dirname $installerDir`
ramaddaDir=${parentDir}/${serviceName}
serverDir=$ramaddaDir/ramaddaserver
serviceScript=${serverDir}/ramaddaService.sh
userdir=$parentDir
promptUser=1
yumArg=""

mkdir -p  ${ramaddaDir}

ramaddaDownload="https://geodesystems.com/repository/release/latest/ramaddaserver.zip"

a

serviceDir="/etc/rc.d/init.d"
basedir=""


usage() {
    echo "installer.sh -os redhat -y (assume yes installer) -help "
    exit
}

header() {
    local msg="$1"
    printf "\n*** ${msg} ***\n";
}


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
	    yumArg=--assumeyes
	    ;;
	*)
	    echo "Unknown argument $1"
	    usage
	    ;;
    esac
    shift
done

#echo "target os: $os"

if [ "$os" == "${OS_REDHAT}" ]; then
    pgsql=pgsql
    pgService=postgresql-server
    pgInstall=http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-redhat93-9.3-1.noarch.rpm
else
    pgsql=pgsql
    pgService=postgresql
    pgInstall=postgresql-server
fi

postgresDir=/var/lib/${pgsql}
postgresDataDir=${postgresDir}/data


yumInstall() {
    local target="$1"
    if [ "$yumArg" == "" ]; then
	yum install ${target}
    else 
	yum install ${yumArg} ${target}
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

mntDir=""

header  "Volume Installation";

declare -a dirLocations=("/dev/xvdb" )
for i in "${dirLocations[@]}"
do
    if [ -b "$i" ]; then
        askYesNo  "Do you want to mount the volume: $i "  "y"
        if [ "$response" == "y" ]; then
            mntDir="$i"
            break;
        fi
    fi
done


##/dev/xvdb       /mnt/ramadda   ext4    defaults,nofail        0       2
while [ "$mntDir" == "" ]; do
    ask  "Enter the volume to mount, e.g., /dev/xvdb  [<volume>|n] "  ""
    if [ "$response" == "" ] ||  [ "$response" == "n"  ]; then
        break;
    fi
    if [ -b $response ]; then
        mntDir="$response"
        break;
    fi
    echo "Volume does not exist: $response"
done

if [ "$mntDir" != "" ]; then
    basedir=/mnt/ramadda
    mntState=$( file -s $mntDir );
    case $mntState in
	*files*)
	    echo "$mntDir is already mounted";
	    ;;
	*)
	    echo "Mounting $basedir on $mntDir"
	    if [ ! -f /etc/fstab.bak ]; then
		cp  /etc/fstab /etc/fstab.bak
	    fi
	    sed -e 's/.*$basedir.*//g' /etc/fstab | sed -e 's/.*added ramadda.*//g' > dummy.fstab
	    mv dummy.fstab /etc/fstab
	    printf "\n#added by ramadda installer.sh\n${mntDir}   $basedir ext4 defaults,nofail   0 2\n" >> /etc/fstab
	    mkfs -t ext4 $mntDir
	    mkdir $basedir
	    mount $mntDir $basedir
	    mount -a
	    ;;
    esac
fi




dfltDir="";
if [ -d "${userdir}" ]; then
    dfltDir="${userdir}/ramadda";
fi


if [ -d "/mnt/ramadda" ]; then
    dfltDir="/mnt/ramadda";
fi

while [ "$basedir" == "" ]; do
    ask   "Enter base directory: [$dfltDir]:" $dfltDir  "The base directory holds the repository and pgsql sub-directories"
    if [ "$response" == "" ]; then
        break;
    fi
    basedir=$response;
    break
done


homedir=$basedir/repository
mkdir -p $homedir

install_password="${RANDOM}_${RANDOM}"

printf  "ramadda.install.password=${install_password}" > ${homedir}/install.properties



parentDir=`dirname $basedir`
permissions=$(stat -c %a $parentDir)
if [ "$permissions" == "700" ]; then
    chmod 755 "$parentDir"
fi



echo "Installing base packages - wget, unzip & java. Fixing the localhost name problem"
yum install -y wget > /dev/null
yum install -y unzip > /dev/null
yum install -y java > /dev/null
sed -e 's/HOSTNAME=localhost.localdomain/HOSTNAME=ramadda.localdomain/g' /etc/sysconfig/network> dummy.network
mv dummy.network /etc/sysconfig/network
sed -e 's/127.0.0.1   localhost localhost.localdomain/127.0.0.1 ramadda.localdomain ramadda localhost localhost.localdomain/g' /etc/hosts> dummy.hosts
mv dummy.hosts /etc/hosts





header   "Database Installation";
### Database 
askYesNo  "Install Postgres database"  "y"
if [ "$response" == "y" ]; then

    yum install -y  ${pgInstall} > /dev/null
    pgdir="${basedir}/${pgsql}"

    postgresql-setup initdb

    if  [ -d ${postgresDir} ] ; then
	if  [ ! -h ${postgresDir} ]; then
	    echo "Moving ${postgresDir} to $pgdir"
	    mv  ${postgresDir} $pgdir
	    ln  -s -f  $pgdir ${postgresDir}
	    chown -R postgres ${postgresDir}
	    chown -R postgres ${pgdir}
	fi
    else
	echo "Warning: ${postgresDir} does not exist"	
    fi


    if [ "$os" == "${OS_REDHAT}" ]; then
	systemctl enable postgresql
	systemctl start postgresql.service
    else
	chkconfig ${pgService} on
	service ${pgService} start
    fi



    if [ ! -f ${postgresDataDir}/pg_hba.conf.bak ]; then
        cp ${postgresDataDir}/pg_hba.conf ${postgresDataDir}/pg_hba.conf.bak
    fi

    postgresPassword="password$RANDOM-$RANDOM"
    postgresUser="ramadda"
    postgresAuth="
#
#written out by the RAMADDA installer
#
host repository ${postgresUser} 127.0.0.1/32  password
local   all             all                                     peer
host    all             all             127.0.0.1/32            ident
host    all             all             ::1/128                 ident
"


    printf "${postgresAuth}" > ${postgresDataDir}/pg_hba.conf

    service ${pgService} reload

    printf "create database repository;\ncreate user ramadda;\nalter user ramadda with password '${postgresPassword}';\ngrant all privileges on database repository to ramadda;\n" > /tmp/postgres.sql
    chmod 644 /tmp/postgres.sql
    echo "Creating repository database and Adding ramadda user"
    su -c "psql -f /tmp/postgres.sql"  - postgres > /dev/null
    rm -f ${installerDir}/postgres.sql
    printf "ramadda.db=postgres\nramadda.db.postgres.user=ramadda\nramadda.db.postgres.password=${postgresPassword}"  > ${homedir}/db.properties
fi


header  "RAMADDA Installation"

askYesNo "Download and install RAMADDA from Geode Systems"  "y"
if [ "$response" == "y" ]; then
    rm -f ${installerDir}/ramaddaserver.zip
    echo "Downloading RAMADDA from ${ramaddaDownload}"
    wget -O ${installerDir}/ramaddaserver.zip ${ramaddaDownload}
    rm -r -f ${serverDir}
    unzip -d ${ramaddaDir} -o ${installerDir}/ramaddaserver.zip
    ramaddaConfig="##
## Generated by the RAMADDA installer
##

#RAMADDA home directory
export RAMADDA_HOME=${homedir}

#Port RAMADDA runs on
export RAMADDA_PORT=80
"

    printf "$ramaddaConfig" > ${serverDir}/ramaddaenv.sh

    askYesNo "Install RAMADDA as a service"  "y"
    if [ "$response" == "y" ]; then
        printf "#!/bin/sh\n# chkconfig: - 80 30\n# description: RAMADDA repository\n\nsh ${serviceScript} \"\$@\"\n" > ${serviceDir}/${serviceName}
        chmod 755 ${serviceDir}/${serviceName}
        chkconfig ${serviceName} on
        printf "To run: sudo service ${serviceName} start|stop|restart\n"
    fi
fi



host=`ifconfig | grep inet | grep cast | awk '/inet addr/{print substr($2,6)}'`
if [ "$host" == "" ]; then
    host=`ifconfig | grep inet | grep cast | awk '/.*/{print $2}'`
fi


header "SSL Configuration";
printf "We need the public IP address to configure SSL. It looks like the  public IP for this machine is ${host}?\n"
read -p "Is that correct?  If you're running in Amazon hit 'n' - [y|n]: " response
if [ "$response" == "n" ]; then
    read -p "Enter the IP address? Enter <return> if you are running in Amazon: " host
    if [ "$host" == "" ]; then
        host=`curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null`
        printf "OK, the public IP of this machine is ${host}\n"
    fi
fi

printf "A self-signed SSL certificate can be created for the IP address ${host}\nThis will enable you to access your server securely but you will need to add a real certificate or add other entries to the keystore for other domain names\n";
askYesNo "Generate keystore and enable SSL" "y"
if [ "$response" == "y" ]; then
    password="ssl_${RANDOM}_${RANDOM}_${RANDOM}"
    echo "Generating new keystore file: ${homedir}/keystore  for host: $host.\nThe password is stored in ${homedir}/ssl.properties"
    rm -f ${homedir}/keystore
    printf "${password}\n${password}\n${host}\nRAMADDA\nRAMADDA\ncity\nstate\ncountry\nyes\n\n" | keytool -genkey -keyalg RSA -alias ramadda -keystore ${homedir}/keystore > /dev/null 2> /dev/null
    printf "#generated password\n\nramadda.ssl.password=${password}\nramadda.ssl.keypassword=${password}\nramadda.ssl.port=443\n" > ${homedir}/ssl.properties
    printf "\nIf you need to create a new key then delete ${homedir}/keystore and run:\n    keytool -genkey -keyalg RSA -alias ramadda -keystore ${homedir}/keystore\nIf you are installing your own certificate then generate the keystore and copy it to ${homedir}"
fi


header "Installation complete";
printf "RAMADDA is installed. \n\tRAMADDA home directory: ${homedir}\n\tPostgres directory: ${postgresDir}\n\tLog file: ${ramaddaDir}/ramadda.log\n\tService script: ${serviceScript}\n"


printf "\n"
printf "Finish the configuration at https://${host}/repository\n"
printf "The installation password is ${install_password}\n"
printf "\n"

service ${serviceName} restart

exit














