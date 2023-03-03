#!/bin/sh
dir=`dirname $0`
java -jar ${dir}/pdfbox-app-2.0.27.jar "$@"
