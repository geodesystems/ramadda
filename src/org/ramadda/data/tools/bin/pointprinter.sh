#!/bin/sh

dirname=`dirname $0`

java -Xmx1024m -jar ${dirname}/lib/pointprinter.jar "$@"
