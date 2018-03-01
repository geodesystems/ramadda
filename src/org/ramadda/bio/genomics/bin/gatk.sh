

gatkdir=`dirname $0`

java -Xmx2000m -jar ${gatkdir}/GenomeAnalysisTK.jar "$@"