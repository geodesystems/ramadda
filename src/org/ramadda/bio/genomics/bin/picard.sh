
dir=`dirname $0`
jar=$1
shift
java -Xmx2000m -jar ${dir}/$jar "$@"
