#!/bin/sh
mydir=`dirname $0`
alias jindent='/Applications/Jindent/Jindent -p ${mydir}/style.xjs '
jindent `find bio -name "*.java"`
jindent `find data -name "*.java"`
jindent `find dev -name "*.java"`
jindent `find geodata -name "*.java"`
jindent `find plugins -name "*.java"`
jindent `find projects -name "*.java"`
jindent `find repository -name "*.java"`
jindent `find service -name "*.java"`
jindent `find test -name "*.java"`
jindent `find util -name "*.java"`
