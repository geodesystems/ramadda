#!/bin/sh

#This script uses https://letsencrypt.org and https://certbot.eff.org/ to create and install a SSL certificate for RAMADDA
#
#You need to have the following packages installed:
#certbot-auto:  https://certbot.eff.org/docs/install.html#certbot-auto
#openssl: https://www.openssl.org/
#keytool: part of a standard JAVA-JDK install

#usage:
#letsencrypt.sh -help"


#If no password specified then this script looks in your RAMADDA home directory in each 
#.properties file for the ssl password of the form (remove '#'):
#ramadda.ssl.password=some password


######################################################################################################
## certbot 
##This script uses certbot-auto. However, it seems like on some AWS instances certbot-auto doesn't run
##From this doc:
##https://serverfault.com/questions/890212/looking-for-a-way-to-get-certbot-running-on-amazon-linux-2
##To install certbot do:
##  curl -O http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
##  sudo yum install epel-release-latest-7.noarch.rpm
##  sudo yum install certbot
##
## and run with makekeystore.sh -certbot /path/to/certbot
######################################################################################################

##params set from args

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
#CERTBOT=${BIN_DIR}/certbot-auto
#CERTBOT=/usr/local/bin/certbot-auto
CERTBOT=/bin/certbot
if [ ! -e "$CERTBOT" ]; then
    CERTBOT=/bin/certbot-3
fi

if [ ! -e "$CERTBOT" ]; then
    CERTBOT=/usr/bin/certbot-3
fi


WHAT=new
RAMADDA_HOME=/mnt/ramadda/ramaddahome
HTDOCS=
FIRST_DOMAIN=
OTHER_DOMAINS=
PASSWORD=


function usage()
{
    printf "usage:\n"
    printf "makekeystore.sh\n"
    printf "\t-new (create new certificate)\n"
    printf "\t-renew (renew certificate)\n"
    printf "\t-certbot /path/to/certbot\n"
    printf "\t-domain <mydomain.com> (specify the domain)\n"
    printf "\t-other  <www.mydomain.com,someotherdomain.com> (specify other domains)\n"        
    printf "\t-home /mnt/ramadda/repository (specify RAMADDA home dir)\n"
    printf "\t-webroot <directory to top-level web root> \n"    
    printf "\t-password <password>  (keystore password. needs to be set in RAMADDA home dir)\n"

}


promptUser=1

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




while [[ $# -gt 0 ]]
do
    arg=$1
    case $arg in
        -new)
            WHAT=new
	    shift
            ;;
        -renew)
            WHAT=renew
	    shift
            ;;
        -dryrun)
            WHAT=dryrun
	    shift
            ;;		
        -home)
	    shift
            RAMADDA_HOME="$1"
	    shift
            ;;
        -password)
	    shift
            PASSWORD="$1"
	    echo "Using password from command line. Don't forget to set it in a RAMADDA home properties file"
	    shift
            ;;	
        -domain)
	    shift
            FIRST_DOMAIN="$1"
	    shift
            ;;		
        -other)
	    shift
            OTHER_DOMAINS="$1"
	    shift
            ;;		
	-certbot)
	    shift
	    CERTBOT="$1"
	    shift
	    ;;
	-webroot)
	    shift
	    HTDOCS="$1"
	    shift
	    ;;	
	-help)
	    usage
	    exit 1
	    ;;
	*)
	    echo "Unknown argument:$arg"
	    usage
	    exit 1
	    ;;
    esac
done


if [ ! -e "$CERTBOT" ]; then
    echo "Cannot find certbot: $CERTBOT"
    echo "Specify the path with -certbot <path to certbot>"
    usage
    exit
fi


if [ -z "$FIRST_DOMAIN" ]; then
    echo "You must specify a domain"
    usage
    exit
fi


if [ ! -d "$RAMADDA_HOME" ]; then
    echo "Cannot find RAMADDA home directory: $RAMADDA_HOME"
    echo "Specify the path with -home <RAMADDA Home>"
    usage
    exit
fi



if [ "$HTDOCS" == "" ]; then
    HTDOCS="${RAMADDA_HOME}/htdocs"
fi

if [ ! -d "$HTDOCS" ]; then
    echo "Cannot find htdocs directory: $HTDOCS"
    echo "Specify the path with -webroot <path to htdocs directory>"
    usage
    exit
fi
    

##If no password then find the properties file that contains the ssl passwords
if [ -z "$PASSWORD" ]; then
    PROPERTY_FILES=${RAMADDA_HOME}/*.properties
    for f in $PROPERTY_FILES
    do
	PASSWORD=`cat $f | egrep "^ramadda.ssl.password" | cut -d'=' -f2`
	if [ "$PASSWORD" ]; then
	    break
	fi
    done
fi


if [ -z "$PASSWORD" ]; then
    echo "No password specified and could not find it in $RAMADDA_HOME"
    exit 1
fi


printf "Creating certificate with:\n"
printf "\twhat:${WHAT}\n"
printf "\tdomain:${FIRST_DOMAIN}\n"
printf "\tother domains:${OTHER_DOMAINS}\n"
printf "\thome dir:${RAMADDA_HOME}\n"
printf "\tweb root:${HTDOCS}\n"

ask   "Create the certificate? [y|n]"  "y"
if [ "$response" != "y" ]; then
    exit
fi


KEYSTORE=keystore.jks
SRCKEYSTORE=keystore.pkcs12

case ${WHAT} in
    ""|"renew")
	${CERTBOT} --debug renew 
	;;
    "new")
	if [ -z "$OTHER_DOMAINS" ]; then
	    ${CERTBOT}  --debug certonly --webroot -w ${HTDOCS} -d ${FIRST_DOMAIN}
	else
	    ${CERTBOT}  --debug certonly --webroot -w ${HTDOCS} -d ${FIRST_DOMAIN} -d ${OTHER_DOMAINS}
	fi
	;;
    "dryrun")
	if [ -z "$OTHER_DOMAINS" ]; then
	    ${CERTBOT} --dry-run  --debug certonly --webroot -w ${HTDOCS} -d ${FIRST_DOMAIN}
	else
	    ${CERTBOT} --dry-run  --debug certonly --webroot -w ${HTDOCS} -d ${FIRST_DOMAIN} -d ${OTHER_DOMAINS}
	fi
	;;
esac


#At some time certbot used 0003 
LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}-0003"
if [ ! -d "$LETSENCRYPT_DIR" ]; then
    LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}-0002"
fi
if [ ! -d "$LETSENCRYPT_DIR" ]; then
    LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}-0001"
fi
if [ ! -d "$LETSENCRYPT_DIR" ]; then
    LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}"
fi

echo "letsencrypt dir: ${LETSENCRYPT_DIR}"

openssl pkcs12 -export -out ${SRCKEYSTORE} -in ${LETSENCRYPT_DIR}/fullchain.pem  -inkey ${LETSENCRYPT_DIR}/privkey.pem  -password pass:${PASSWORD}
rm -f ${KEYSTORE}
echo "$PASSWORD
$PASSWORD
$PASSWORD
" | keytool -importkeystore -srckeystore ${SRCKEYSTORE} -destkeystore ${KEYSTORE} -deststoretype pkcs12
#rm -f ${SRCKEYSTORE}

cp ${KEYSTORE} ${RAMADDA_HOME}

echo "Certificate created and stored in ${RAMADDA_HOME}"

ask   "Do you want to restart RAMADDA? [y|n]"  "y"
if [ "$response" != "y" ]; then
    echo "OK, don't forget to restart RAMADDA"
    exit
fi

echo "OK, calling service ramadda start"
service ramadda start


