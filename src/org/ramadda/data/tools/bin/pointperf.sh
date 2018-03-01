#!/bin/sh

dirname=`dirname $0`

java -server -Xmx1024m -jar ${dirname}/lib/pointperf.jar "$@"
