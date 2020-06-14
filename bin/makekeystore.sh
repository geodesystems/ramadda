#!/bin/sh

#This script uses https://letsencrypt.org and https://certbot.eff.org/ to create and install a SSL certificate for RAMADDA
#You need to have certbot-auto installed:
# https://certbot.eff.org/docs/install.html#certbot-auto

#usage:
#makekeystore.sh new -> make a new certificate
#makekeystore.sh renew -> renew the certificates (default)


#This script looks in your RAMADDA home directory for either an ssl.properties or repository.properties file
#for the ssl password of the form (remove '#'):
#ramadda.ssl.password=some password


########################################################################################################
## certbot 
##This script uses certbot-auto. However, it seems like on some AWS instances certbot-auto doesn't run
##From this doc:
##https://serverfault.com/questions/890212/looking-for-a-way-to-get-certbot-running-on-amazon-linux-2
##To install certbot do:
## curl -O http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
## sudo yum install epel-release-latest-7.noarch.rpm
## sudo yum install certbot
##
## and run with makekeystore.sh -certbot /path/to/certbot


##params set from args

BIN_DIR=`dirname $0`
CERTBOT=${BIN_DIR}/certbot-auto
WHAT=new
RAMADDA_HOME=/mnt/ramadda/repository 
FIRST_DOMAIN=
OTHER_DOMAINS=


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
        -home)
	    shift
            RAMADDA_HOME="$1"
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



if [ -z "$FIRST_DOMAIN" ]; then
    echo "You must specify a domain"
    usage
    exit
fi


##Hard code the geodesystems extra domains
if [ "${FIRST_DOMAIN}" = "geodesystems.com" ]; then
    OTHER_DOMAINS=www.geodesystems.com,boulderdata.org,www.boulderdata.org,communidata.org,www.communidata.org,10000cities.org,www.10000cities.org,ramadda.org,www.ramadda.org
fi



echo "running with:"
echo "what:${WHAT}"
echo "domain:${FIRST_DOMAIN}"
echo "other domains:${OTHER_DOMAINS}"
echo "home dir:${RAMADDA_HOME}"
exit


PROPERTY_FILE=${RAMADDA_HOME}/repository.properties

if test -f "${RAMADDA_HOME}/ssl.properties"; then
    PROPERTY_FILE=${RAMADDA_HOME}/ssl.properties
fi


PASSWORD=`cat $PROPERTY_FILE | egrep "^ramadda.ssl.password" | cut -d'=' -f2`
KEYSTORE=keystore.jks
SRCKEYSTORE=keystore.pkcs12

case ${WHAT} in
    ""|"renew")
##	${CERTBOT} --dry-run --debug renew 
	${CERTBOT} --debug renew 
	;;
    "new")
##	${CERTBOT}  --dry-run --debug certonly --webroot -w ${RAMADDA_HOME}/htdocs -d ${FIRST_DOMAIN} -d ${OTHER_DOMAINS}
	if [ -z "$OTHER_DOMAINS" ]; then
	    ${CERTBOT}  --debug certonly --webroot -w ${RAMADDA_HOME}/htdocs -d ${FIRST_DOMAIN}
	else
	    ${CERTBOT}  --debug certonly --webroot -w ${RAMADDA_HOME}/htdocs -d ${FIRST_DOMAIN} -d ${OTHER_DOMAINS}
	fi
	;;
esac


#Check for the -0003 dir. Note sure why certbot makes this
LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}-0003"
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
echo "Don't forget to retart your RAMADDA"

