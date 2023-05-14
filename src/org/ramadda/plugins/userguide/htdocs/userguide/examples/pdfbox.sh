#
#Just a shell wrapper we use to call pdfbox
#

dir=`dirname $0`
java -jar ${dir}/pdfbox-app-1.8.7.jar "$@"
