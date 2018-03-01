set ::bin [file dirname [info script]]
set ::dev [file dirname $::bin]
set ::resourcedir [file join $::bin resources]
set ::pointtoolsdir [file join $::dev pointtools]


proc copyFile {from to} {
    if {[file exists $to]} {
	return 0;
    }
    puts "Copying file [file tail $from] to  $to"
    file copy -force $from $to
    return 1
}



proc writeFile {file contents} {
    set fp [open $file w]
    puts $fp $contents
    close $fp
}
