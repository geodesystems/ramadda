
##
## Call this with:
## tclsh filecleanup.tcl args  <directories to walk>
## 

##
##This script recurses down a given set of directories and replaces spaces with "_" in file and directory names
##Any .RiSCAN directories are zipped up and the original directory is removed
##


set ::spaces 0
set ::dates 0
set ::test 1



proc getFlag {flag dflt}  {
    if {[info exists ::flags($flag)]} {
        return $::flags($flag)
    }
    return $dflt
}

proc checkFile {f patterns zipPatterns unzipPatterns skip} {


# Clean up the name
    set newFile $f

    foreach tuple $patterns {
        foreach {from to} $tuple break
        regsub -all $from $newFile $to newFile
    }

#    puts "FILE:  $newFile"
    if {$::spaces} {
        #change spaces for "_"
	set tail [file tail $newFile]
	##Only look at the tail 
        if {[regexp {\s} $tail]} {
	    regsub -all {\s+} $newFile _ newFile
#	    puts "SPACE $newFile"
	    regsub -all {_-_} $newFile _ newFile
	}
    }

    if {$::dates} {
        set exp {(.*|^)(20(08|09|10|11|12))-(01|02|03|04|05|06|07|08|09|10|11|12)-([012]\d|30|31)(.*)}
        regsub  $exp $newFile {\1\2_\4_\5\6} newFile

        #Check for yyyymmdd and convert it to yyyy_mm_dd
        set exp {(.*|^)(20(08|09|10|11|12))(01|02|03|04|05|06|07|08|09|10|11|12)([012]\d|30|31)(.*)}
        regsub  $exp $newFile {\1\2_\4_\5\6} newFile
    }


#If the name changed then move the file
    if {$f != $newFile} {
        if {$::test} {
            puts "test: renaming $f to $newFile"
        } else {
	    if {[file exists $newFile]} {
		puts "ERROR: Trying to copy to existing file: $newFile"
		return;
	    }
            puts "renaming $f to $newFile"
            file rename -force $f $newFile
            set f $newFile
        }
    }

        #If its one of the zip patterns then zip it up and return
        set unzipit 0
        foreach pattern $unzipPatterns {
            if  {[regexp $pattern $f]} {
                set unzipit 1
                break
            }
        }
        if {$unzipit} {
            set dir [file dirname $f]
            set name [file tail $f]
            set zipName $name.zip
	    regsub -all {\s} $zipName {_} zipName
            if {$::test} {
                puts "test: Unzipping file: $f"
                puts "test: Deleting original file"
            } else {
                puts "Unipping file: $f"
                set cwd [pwd]
                cd [file dirname $f]
                exec jar  -xvf [file tail $f]
                puts "Deleting original file"
                file delete -force [file tail $f]
                cd $cwd
            }
            return
        }


    if  {[file isdirectory $f]} {
        foreach pattern $skip {
            if  {[regexp $pattern $f]} {
#                puts "skipping: $f"
                return
            }
        }

        #If its one of the zip patterns then zip it up and return
        set zipit 0
        foreach pattern $zipPatterns {
            if  {[regexp $pattern $f]} {
                set zipit 1
                break
            }
        }
        if {$zipit} {
            set dir [file dirname $f]
            set name [file tail $f]
            set zipName $name.zip
	    regsub -all {\s} $zipName {_} zipName
            if {$::test} {
                puts "test: Zipping  directory: $f"
                puts "test: Deleting original directory"
            } else {
                puts "Zipping  directory: $f"
#The -0 says to not compress. This is much faster
                exec jar  -cvfM $dir/$zipName  -C $dir $name
                puts "Deleting original directory"
                file delete -force $f
            }
            return
        }



#Recurse down the tree
        foreach child [glob -nocomplain $f/*] {
            checkFile $child $patterns $zipPatterns $unzipPatterns  $skip
        }
    }
}

set patterns [list]
set zipPatterns  [list]
set unzipPatterns  [list]
set skip  [list]


for {set i 0} {$i<$argc} {incr i} {
    set arg [lindex $argv $i]
    if {$arg =="-help"} {
        puts "usage: filecleanup.tcl <-doit> <-convert from to> <-zip pattern> <-skip pattern> \[directories to recurse\]"
        exit
    }

    if {$arg =="-convert"} {
        incr i
        set from [lindex $argv $i]
        incr i
        set to [lindex $argv $i]
        lappend patterns [list $from $to]
   } elseif {$arg =="-doit"} {
       set ::test 0
   } elseif {$arg =="-spaces"} {
       set ::spaces 1
   } elseif {$arg =="-dates"} {
       set ::dates 1
    } elseif {$arg =="-zip"} {
        incr i
        lappend zipPatterns [lindex $argv $i]
    } elseif {$arg =="-unzip"} {
        incr i
        lappend unzipPatterns [lindex $argv $i]
    } elseif {$arg =="-skip"} {
        incr i
        lappend skip [lindex $argv $i]
    } else {
        if {![file exists $arg]} {
            puts "Directory does not exist: $arg"
            exit
        }
        checkFile $arg $patterns $zipPatterns $unzipPatterns $skip
    }
}