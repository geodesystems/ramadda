

set ::doExtract 1

proc processFile {f} {
    set fp [open $f r]
    set contents [read $fp]
    close $fp
    if {$::doExtract} {
        foreach m {msg msgLabel} {
            set c $contents
            set pattern "$m\\s*\\(\\s*\"(\[^\"\]+)\"(.*)"
            while {[regexp $pattern $c match tok rest]} {
                set c $rest
                if  {[info exists ::seen($tok)]} {
                    continue;
                }
                set ::seen($tok) 1
                lappend ::toks $tok
            }
        }
    }
    if {!$::doExtract} {
        foreach m {msg msgLabel} {
            foreach c [split $contents "\n"] {
                set line $c
                set pattern "$m\\s*\\((\[^\"\\)\]+)(.*)"
                while {[regexp $pattern $c match tok rest]} {
                    puts "$f [string trim $line]"
                    set c $rest
                }
            }
        }
    }
}

proc bylength {v1 v2} {
    set l1   [string length $v1]
    set l2   [string length $v2]
    if {$l1<$l2} {return -1;}
    if {$l1>$l2} {return 1;}
    return 0
}



set ::toks [list]
foreach f  $argv {
    processFile $f
}

set ::toks  [lsort -command bylength $::toks]

set comments {

################################################################################################
#
#This is an example language translation file. All of the phrases below were
#extracted from the RAMADDA source code.
#
#RAMADDA looks in the org/ramadda/repository/resources/languages package for
#any file that ends with ".pack". It then looks in the repository plugins directory
#for any .pack files. So, rename this file, (e.g., sp.pack) copy it to your plugins and put your 
#translations in it below. If you see a phrase in RAMADDA that is not getting translated
#just send a note about it to the developers. 
#
#The following 2 lines define the id of the language and its name
#Change them for your language

language.id=example
language.name=Example Language 

#
# To set a language as the default language for the entire site
# define the following property in your local repository.properties file or
# in the Admin->Site and Contact Information -> Extra Properties section
#
# ramadda.language.default=example
#

#
#Here is where you do the translation
#.e.g:
#Phrase used in RAMADDA=Your translation
#If you don't have a translation then RAMADDA will just use the English phrase
#
################################################################################################
}

if {$::doExtract} {
    puts $comments
    foreach tok $::toks {
        puts "$tok="
    }
}