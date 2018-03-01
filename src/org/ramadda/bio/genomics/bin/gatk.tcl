
proc processAttrs {s} {
#    @Argument(fullName = "variant_index_parameter", doc = "the parameter (bin width or features per bin) to pass to the VCF/BCF IndexCreator", required = false)
    array set map []
    foreach tok [split $s ","] {
        set name [string trim [lindex [split $tok "="] 0]]
        set value  [string trim [lindex [split $tok "="] 1]]
        regsub -all {^"} $value {} value
        regsub -all {"$} $value {} value
##        puts "attr: $name=$value"
        set map($name) $value
    }
    return [array get map]

}


proc processFile {file} {
    set fp [open $file  r]
    set c [read $fp]
    close $fp

    regsub -all {(@Argument\s*\([^\)]+)\n(.*\))}  $c {\1\2} c
    set help ""
    if {[regexp {<h3>Example</h3>.*?<pre>(.*)</pre>} $c match help]} {
        regsub -all {^\s*\*} $help {} help
        regsub -all { \* } $help {} help
    }



    set isFeature 0
    set class ""
    set package "";
    set lastLineArg 0
    set lastLineInput 0
    set lastLineOutput 0
    set lastLineAttrs ""
    set inHidden 0 

    foreach line [split $c "\n"] {
        set line [string trim $line]
        if {$line == ""} continue;

        if {$package == ""} {
            if {[regexp {.*package\s+(.*);.*} $line match package]} {
                continue;
            }
        }

        if {!$isFeature} {
            if {[regexp {@DocumentedGATKFeature\((.*)\)} $line match featureAttrs]}  {
                set isFeature 1
                
            }
            continue;
        }

        if {$class == ""} {
            if {[regexp {public\s+(abstract)*class\s+([^\s]+)\s} $line match skip class]} {
                if {$class == "CommandLineGATK"} {
#                    puts stderr "Skipping $file";
#                    return;
                }
                puts "<outputhandler class=\"org.ramadda.repository.output.ExecutableOutputHandler\" >"                
                puts "<command  id=\"gatk_[string tolower $class]\"  icon=\"/genomics/dna.png\" command=\"\${bio.gatk}\"  label=\"Run GATK $class\" >"
                puts "<arg value=\"$class\"  prefix=\"--analysis_type\"/>"
                if {$package!=""} {
                    regsub -all {\.} $package {_} package
                    set help "<a href=\"http://www.broadinstitute.org/gatk/gatkdocs/${package}_$class.html\" target=_documentation>Documentation</a><br>$help"
                }

                puts "<help><!\[CDATA\[<pre>$help</pre>\]\]></help>"
                continue;
            }
        }

        if {$class == ""} {
            continue;
        }

        if {!$isFeature} {
            puts stderr "Is not a documented feature"
            return
        }

        if {[regexp {\s*@Hidden.*} $line ]} {
            set inHidden 1
            continue;
        }

        if {$lastLineInput} {
            puts stderr "input: $lastLineAttrs"
#            puts stderr "line: $line"
            array set attrs [processAttrs $lastLineAttrs]
            set lastLineInput 0
            continue
        }

        if {$lastLineOutput} {
#           puts stderr "output: $file"
#           puts stderr "line: $line"
            array set attrs {required 0 fullName ""}
            array set attrs [processAttrs $lastLineAttrs]
            set lastLineOutput 0

            if {[regexp File $line]} {
                puts stderr "Output: $lastLineAttrs"
                puts "<arg prefix=\"--$attrs(fullName)\" value=\"\${entry.filebase}.out\"><help>$attrs(doc)</help></arg>";
            }
            continue
        }

        if {$lastLineArg} {
            set lastLineArg 0
            set multiples 0
            if {$inHidden} {
                set inHidden 0
                continue
            }
            if {![regexp {(private|protected)\s+([^\s]+)\s} $line match skip type]} {
                if {![regexp {(public|private|\s*)\s*(byte|int|Integer|float|double|String|boolean)\s+([^\s]+).*} $line match skip type]} {
                    if {[regexp {(List<String>|Set<String>)} $line match type]} {
                        set type String
                        set multiples 1
                    } elseif {[regexp {.*VariantContextWriter.*} $line match type]} {
                        puts stderr "Is VC writer";
                        continue;
                    } elseif {[regexp {.*OutputFormat.*} $line match type]} {
                        puts stderr "Output format: $line";
                        continue;
                    } else {
#                        puts stderr "??: $line"
#                        puts stderr "File: $file"
                        continue;
                    }
                }
            }
#            puts stderr "$file  $lastLineAttrs"
            array set attrs [processAttrs $lastLineAttrs]
            set fullName ""
            set doc ""
            if {[info exists attrs(doc)]} {
                set doc $attrs(doc)
                regsub -all \" $doc {} doc
                regsub -all {\s\s+} $doc { } doc
                regsub -all {\+} $doc {} doc
                regsub -all {<} $doc {\&lt;} doc
                regsub -all {>} $doc {\&gt;} doc
            }


            if {[info exists attrs(fullName)]} {
                set fullName $attrs(fullName)
                regsub -all {\s} $fullName {} fullName
                regsub -all \" $fullName {} fullName
                regsub -all {\+} $fullName {} fullName
            }

            if {[info exists attrs(shortName)]} {
                set shortName $attrs(shortName)
            } else {
                set shortName $fullName
            }
            if {$fullName == "" } {
                set fullName $shortName
            }


            set required 0;
#            puts stderr $file
            if {[info exists attrs(required)]} {
                set required $attrs(required)
            }
            set lastLineArg 0
##     <arg type="flag" label="Output unaligned reads along with aligned reads">--unaligned</arg>
            set commandType string
            set label $fullName
            regsub -all {([A-Z])} $label { \1} label
            regsub -all {_} $label { } label
            set label "[string toupper [string range $label 0 0]][string range $label 1 end]"
            set extra ""
            if {$doc!=""} {
                 append extra "<help>$doc</help>"
            }
            set extraAttrs ""

           if {$required} {
               append extraAttrs " required=\"true\" "
           }
            if {[string tolower $type] == "boolean"} {
                set commandType flag
                puts "<arg type=\"$commandType\" $extraAttrs label=\"$label\" value=\"--$fullName\">$extra</arg>";
            } else {
                if {$type == "Integer"} {
                    set commandType int
                }
                puts "<arg type=\"$commandType\" $extraAttrs label=\"$label\" prefix=\"--$fullName\">$extra</arg>";
            }

        }

        if {[regexp {@Argument\((.*)\)} $line match lastLineAttrs]}  {
            set lastLineArg 1
            continue;
        }
        if {[regexp {@Input\((.*)\)} $line match lastLineAttrs]}  {
            set lastLineInput 1
            continue;
        }
        if {[regexp {@Output\((.*)\)} $line match lastLineAttrs]}  {
            set lastLineOutput 1
            continue;
        }
        if {[regexp {@Output} $line match ]}  {
            set lastLineAttrs ""
            set lastLineOutput 1
            continue;
        }




    }

##LOOP


    if {$isFeature} {
        if {$class == ""} {
#            puts stderr "NO CLASS: $file"
            return;
        }

        if {[regexp CommandLineGATK.class $featureAttrs]} {
            puts "<arg value=\"--input_file\"/>";
            puts "<arg type=\"entry\" entryType=\"bio_bam\" primary=\"true\" value=\"\${entry.file}\"></arg>"
            puts "<arg label=\"Exclude Intervals\" suffix=\"One or more genomic intervals to exclude from processing\">--excludeIntervals</arg>"
            puts "<arg label=\"Intervals\" suffix=\"One or more genomic intervals over which to operate\" prefix=\"--intervals\"></arg>"
            puts "<arg label=\"Reference Sequence File\" type=\"entry\" entryType=\"bio_fasta\" prefix=\"--reference_sequence\"></arg>"
        }

        puts "</command>"
        puts "</outputhandler>"
    }



}

puts "<outputhandlers>"
foreach arg $argv {
    processFile $arg
}
puts "</outputhandlers>"

