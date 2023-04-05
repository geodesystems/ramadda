rm -r -f generated; tclsh process.tcl $1; rm -r -f ~/generated; mv generated ~/; jar -cvf ~/generated.zip ~/generated
