

set ::header {
/*
 * Copyright 2013 ramadda.org
 * http://ramadda.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

}


proc isIndexed {class var arraySize} {
    return 0
}

proc camel {var} {
    return [string toupper [string range $var 0 0 ]][string range $var 1 end]
}

proc bitMask {method var index} {
    set var [getVarName $var]
    set code ""
    append code [method "public boolean get${method}()" "return isBitSet($var,$index);"]
    append code [method "public void set${method}(boolean flag)" "$var = setBit($var,$index,flag);"]
    set code
}

proc getVarName {var} {
    set label $var
    regsub -all -- {-} $label {} label
    while {[regsub -all  {  } $label { } label]} {}
    set goodName ""
    set cnt 0
    regsub -all  { } $label {_} label

    set toks  [split $label "_"]
    if {[llength $toks] == 1} {
        return [lindex $toks 0]
    }
#    puts $label
    foreach tok [split $label "_"] {
        if {$cnt==0} {
            append goodName [string tolower $tok]
        } else {
            append goodName [string toupper [string range $tok 0 0]][string tolower [string range $tok 1 end]]
        }
        incr cnt;
    }
    return $goodName
    regsub -all  { } $label {_} var
    set var [string tolower $var]
}

proc getJavaType {type} {
    switch $type {
        i1b {return byte}
        i2b {return short}
        i4b {return int}
        i8b {return long}
        r4b {return float}
        r8b {return double}
    }
    return $type;
}


proc generateRecordClass {class args} {
    array set A {-super {Record} -extraBody {} -extraCopyCtor {} -extraImport {} -fields {} -readPost {} -readPre {} -writePre {} -writePost {} -lineoriented 0 -delimiter {,} -makefile 0 -filesuper {PointFile} -skiplines {0} -capability {} }
    set SUPER [string toupper $A(-super)]
    array set A $args
    set list $A(-fields)
    set baseClass [string equal "$A(-super)"  "Record"]

    set delimiterString $A(-delimiter)
    regsub  -all {\+} $delimiterString {} delimiterString

    regexp {(.*)\.([^\.]+)} $class match package class
    puts "Generating class: $class"

    set javaFile $class.java
    if {$A(-makefile)} {
        set fileClass $class
        regsub -all {Record} $fileClass {File} fileClass
        set javaFile $fileClass.java
    }


    set fp [open $javaFile w]
    puts $fp $::header
    puts $fp "\npackage $package;\n"
    puts $fp "import org.ramadda.data.record.*;"
    puts $fp "import java.io.*;"
    puts $fp "import java.util.ArrayList;"
    puts $fp "import java.util.List;"
    puts $fp "import java.util.HashSet;"

    puts $fp [extraImport]
    puts $fp $A(-extraImport)

    #Assume if we do makefile then we are doing point files
    if {$A(-makefile)} {
        puts $fp "import org.ramadda.data.point.PointFile;"
    }


    puts $fp  "\n\n"
    puts $fp "/** This is generated code from generate.tcl. Do not edit it! */"

    if {$A(-makefile)} {
        puts $fp  "public class $fileClass extends $A(-filesuper) \{"
        puts $fp "public ${fileClass}()  {}"
        puts $fp "public ${fileClass}(String filename) throws java.io.IOException {super(filename);}"
        puts $fp "public Record doMakeRecord(VisitInfo visitInfo) {return new ${class}(this);}"
        puts $fp "public static void main(String\[\]args) throws Exception \{PointFile.test(args, ${fileClass}.class);\n\}\n"
        if {$A(-skiplines)} {
            puts $fp "\n@Override\npublic int getSkipLines(VisitInfo visitInfo) \{\nreturn  $A(-skiplines);\n\}\n\n"
        }

        if {$A(-capability)!=""} {
            puts $fp "@Override\npublic boolean isCapable(String action) {"
            foreach c $A(-capability) {
                puts $fp "if(action.equals($c)) return true;"
            }
            puts  $fp "return super.isCapable(action);"
            puts $fp "}\n"
        }


        puts $fp "\n//generated record class\n\n"
        puts $fp  "public static class $class extends $A(-super) \{"
    } else {
        puts $fp  "public class $class extends $A(-super) \{"
    }

    set getters ""
    set writes "$A(-writePre)"
    set readCode $A(-readPre)
    if {$A(-lineoriented)} {
#        append getters "private String fileDelimiter = \"$A(-delimiter)\";\n";
#        append getters [method "public String getFileDelimiter()" " return fileDelimiter;" ]
        set delim $A(-delimiter)
        if {[regexp {\(} $delim] || [regexp {\"} $delim]} {
            append readCode "String\[\] toks = line.split($delim);\nint fieldCnt = 0;\n"
        } else {
            append readCode "String\[\] toks = line.split(\"$delim\");\nint fieldCnt = 0;\n"
        }
    }
    set decl ""
    set getvalue ""

    set recordStatics "static \{\n"


    set statics "public static final int ATTR_FIRST = $A(-super).ATTR_LAST;\n"
    append statics "public static final List<RecordField> FIELDS = new ArrayList<RecordField>();\n"
    set csv "";
    set csvPost "";
    append csv "boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);\n"
    append csv "int superCnt = super.doPrintCsv(visitInfo,pw);\nint myCnt = 0;\n"
    set csvHeader "int superCnt = super.doPrintCsvHeader(visitInfo,pw);\nint myCnt = 0;\n"
    append csvHeader "boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);\n"
    set csvHeaderPost ""

    set print "super.print(buff);\n"
    set ctor2 ""
    set ctor3 ""
    set cnt 0
    set recordSize 0
    set extraRecordSize [list]
    set equals ""

    set testcode ""

    set varCnt 1

    set quickScanSize 0
    set quickScanOn 0

    foreach  tuple $list {
        set dflt ""
        set var [lindex $tuple 0]
        if {$var == "beginquickscan"} {
            set quickScanSize 0
            set quickScanOn 1
            append readCode "if(!getQuickScan()) \{\n"
            continue;
        }
        if {$var == "endquickscan"} {
            append readCode "\} else \{\n"
            append readCode "recordIO.getDataInputStream().skipBytes($quickScanSize);\n\}\n"
            continue;
        }

        set rawType [lindex $tuple 1]

        set type $rawType

        array set A {-synthetic 0 -getter {} -default {} -declare 1 -cast {} -csv {} -valuegetter {}   -indexed 0  -searchable false -searchsuffix {} -bitfields {} -chartable false -scale {1} -label {} -desc {} -unit {} -enums {} -skip 0 -unsigned 0 -missing {}}
        array set A [lrange $tuple 2 end]



        if {[cleanUpVar]} {
            set var [getVarName $var]
        }
        if {$A(-label)==""} {set A(-label) $var}

        if {!$A(-synthetic) && !$A(-skip)} {
            append ctor3 "this.$var = that.$var;\n"
        }
        set type [string trim $type]

        set array 0
        set variableLengthArray 0
        set arraySize 0
        if {[regexp {(.*)\[([0-9]+)\]} $type match type arraySize]} {
            set array 1
        } elseif {[regexp {(.*)\[(.+)\]} $type match type arraySize]} {
            set array 1
            set variableLengthArray 1
        }
        if {[regsub -all {,} $arraySize {*} arraySize]} {
            set arraySize [expr $arraySize]
        }

        set type [getJavaType $type]
        set javaType $type
        if {$A(-unsigned)} {
            if {$type == "byte"} {
                set type ubyte
            } elseif {$type == "short"} {
                set type ushort
            } elseif {$type == "int"} {
                set type uint
            } else {
                puts "ERROR: not handling unsigned type $type"
            }
        }
        set unsigned ""
        if {[regexp {ushort} $type]} {
            set unsigned Unsigned
            set type short
            set javaType int
        }
        if {[regexp {uint} $type]} {
            set unsigned Unsigned
            set type int
            set javaType long
        }
        if {[regexp {ubyte} $type]} {
            set unsigned Unsigned
            set type byte
            set javaType short
        }

        set Type [camel $type]
        set JavaType [camel $javaType]

        if {$A(-scale)!=1} {
            #assume double when scaling
            set type double
        }


        set typeSize 0
        set numBytes 0
        set VAR [string toupper $var]
        set recordAttrName "RECORDATTR_[string toupper ${var}]"

        append statics "public static final int ATTR_$VAR =  ATTR_FIRST + $varCnt;\n"
        incr varCnt;
        if {$array} {
            set isString [string equal $type "string"]
            if {$isString} {
                set type byte
                set javaType byte
                set Type [camel $type]
                set JavaType [camel $javaType]
            }
            
            if {$variableLengthArray} {
                set typeSize 0
		if {[regexp {^[0-9]+$} $arraySize]} {
		    set tmpSize [expr [getTypeSize $type] * $arraySize]
		} else {
		    set tmpSize 0;
		}

                set numBytes $tmpSize
                lappend extraRecordSize $tmpSize
                if {$A(-skip)} {
                    append readCode "//Skipping $var\n"
                    incr quickScanSize $tmpSize
                    append readCode "recordIO.getDataInputStream().skipBytes($tmpSize);\n"
                    continue;
                }
            } else {
                set typeSize [expr $arraySize*[getTypeSize $type]]
                set numBytes $typeSize
                if {$A(-skip)} {
                    append readCode "//Skipping $var\n"
                    incr quickScanSize $typeSize
                    append readCode "recordIO.getDataInputStream().skipBytes($typeSize);\n"
                    continue;
                }

            }


            append equals "if(!java.util.Arrays.equals(this.$var, that.$var)) \{System.err.println(\"bad $var\"); return false;\}\n"
            append testcode "for(int i=0;i<this.$var.length;i++) this.$var\[i\] = (${javaType}) (Math.random()*1000);\n"
            append csvPost "if(includeVector) {\n"
            append csvHeaderPost "if(includeVector) {\n"
            append csvPost "\tfor(int i=0;i<this.$var.length;i++) {pw.print(i==0?'|':',');pw.print(this.$var\[i\]);}\n"
            append csvPost "\tmyCnt++;\n"
            append csvHeaderPost "\tpw.print(',');\n"
            append csvHeaderPost "\t${recordAttrName}.printCsvHeader(visitInfo,pw);\n"
            append csvHeaderPost "\tmyCnt++;\n"
            append csvPost "}\n"
            append csvHeaderPost "}\n"




            if {$A(-declare)} {
                if {$isString && $A(-default)!=""} {
                    append decl "$javaType\[\] $var = createByteArray(\"$A(-default)\", $arraySize);\n"
                } else {
                    if {$variableLengthArray} {
                        append decl "$javaType\[\] $var = null;\n"
                    } else {
                        append decl "$javaType\[\] $var = new $javaType\[$arraySize\];\n"
                    }
                }
                append getters [method "public $javaType\[\] get[camel $var]()" "return $var;"]
	    }
            if {$isString} {
                append decl "String ${var}AsString;\n"
                append getters [method "public String get[camel $var]AsString()" "if(${var}AsString==null) ${var}AsString =new String($var);\nreturn ${var}AsString;"]
            } 
            if {$A(-lineoriented)} {
                error "Cannot handle arrays in line oriented records"
            }

            if {$unsigned!=""} {
                append writes "write${unsigned}${Type}s(dos, $var);\n"
            } else {
                append writes "write(dos, $var);\n"
            }
            if {$variableLengthArray} {
                append readCode "if($var==null || $var.length!=$arraySize) $var = new $javaType\[$arraySize\];\n"
            }
            append readCode "read${unsigned}${Type}s(dis,$var);\n"
            append getters [method "public void set[camel $var]($javaType\[\] newValue)" "copy($var, newValue);"]
            if {$isString} {
                append getters [method "public void set[camel $var](String  newValue)" "copy($var, newValue.getBytes());"]
            }

            if {$isString} {
                append print "buff.append(\" $var: \" + get[camel $var]AsString()+\" \\n\");\n"
            }
            if {$A(-indexed) || [isIndexed  $class $var $arraySize]} {
                if {$cnt} {
                    append csv "pw.print(',');\n"
                    append csvHeader "pw.print(',');\n"
                } else {
                    append csv "if(superCnt>0) pw.print(',');\n"
                    append csvHeader "if(superCnt>0) pw.print(',');\n"
                }
                append csv "pw.print(getStringValue(${recordAttrName}, $var\[getCurrentIndex()\]));\n"
##                append csv "pw.print($var\[getCurrentIndex()\]);\n"
                append csv "myCnt++;\n"
                append csvHeader "${recordAttrName}.printCsvHeader(visitInfo,pw);\n"
                append csvHeader "myCnt++;\n"
                append getvalue "if(attrId == ATTR_$VAR) return $var\[getCurrentIndex()\];\n"
            }
        } else {
            if {$A(-synthetic)} {
                append getvalue "if(attrId == ATTR_$VAR) return $A(-getter)();\n"
                set typeSize 0
            }  else {
                set typeSize [getTypeSize $type]
            }
            set numBytes $typeSize

            if {$A(-skip)} {
                append readCode "//Skipping $var\n"
                incr quickScanSize $typeSize
                append readCode "recordIO.getDataInputStream().skipBytes($typeSize);\n"
                continue;
            }

            if {!$A(-synthetic)} {
                append equals "if(this.$var!= that.$var) \{System.err.println(\"bad $var\");  return false;\}\n"
                append testcode "this.$var = (${javaType}) (Math.random()*1000);\n"
                if {$A(-lineoriented)} {
                    if {$cnt>0} {
                        append writes "printWriter.print(delimiter);\n"
                    }
                    append writes "printWriter.print($var);\n"
                } else {
                    append writes "write${unsigned}${Type}(dos, $var);\n"
                }
                append getvalue "if(attrId == ATTR_$VAR) return $var;\n"
                if {$A(-default) !=""} {
                    if {$A(-declare)} {
                        append decl  "$javaType $var = $A(-default);\n"
                    }
                } else {
                    if {$A(-declare)} {
                        append decl  "$javaType $var;\n"
                    }
                }
                if {$A(-lineoriented)} {
                    append readCode "$var = ($javaType) Double.parseDouble(toks\[fieldCnt++\]);\n"
                    if {$A(-missing) != {}} {
                        append readCode "if(isMissingValue($recordAttrName, $var)) $var = Double.NaN;\n"
                    }
                } else {
                    append readCode "$var = $A(-cast) read${unsigned}${Type}(dis);\n"
                }
                append readCode [extraReadForVar $var $type]
                if {$A(-declare)} {
                    append getters [method "public $javaType get[camel $var]()" "return $var;"]
                    append getters [method "public void set[camel $var]($javaType newValue)" "$var = newValue;"]
                }
                if {$cnt} {
                    append csv "pw.print(',');\n"
                    append csvHeader "pw.print(',');\n"
                } else {
                    append csv "if(superCnt>0) pw.print(',');\n"
                    append csvHeader "if(superCnt>0) pw.print(',');\n"
                }
                if {$A(-csv)!=""} {
                    append csv "$A(-csv);\n"
                } else {
#                    append csv "pw.print($var);\n"
                    append csv "pw.print(getStringValue(${recordAttrName}, $var));\n"
                }
                append csv "myCnt++;\n"
                append csvHeader "${recordAttrName}.printCsvHeader(visitInfo,pw);\n"
                append csvHeader "myCnt++;\n"
                append print "buff.append(\" $var: \" + $var+\" \\n\");\n"
            }
            incr cnt
	}


        append statics "public static final RecordField $recordAttrName;\n"
	set attributeArraySize $arraySize
	if {[regexp {\(} $attributeArraySize]} {
	    set attributeArraySize 0
	}
        if {[string trim $A(-searchable)] == "true"} {
            set searchable SEARCHABLE_YES
        } else {
            set searchable SEARCHABLE_NO
        }
        if {[string trim $A(-chartable)] == "true"} {
            set chartable CHARTABLE_YES
        } else {
            set chartable CHARTABLE_NO
        }
        append recordStatics "FIELDS.add($recordAttrName = new RecordField([dqt $var], [dqt $A(-label)], [dqt $A(-desc)], ATTR_$VAR, \"$A(-unit)\", \"$rawType\", \"$type\", $attributeArraySize, $searchable,$chartable));\n"
 

        if {$A(-missing) !=""} {
            append recordStatics "$recordAttrName.setMissingValue($A(-missing));\n"
        }
        if {$A(-synthetic)} {
            append recordStatics "$recordAttrName.setSynthetic(true);\n"
        }

        if {$attributeArraySize==0 && !$array} {
            if {$A(-synthetic)} {
                set getter "$A(-getter)()"
            } else {
                set getter "$var"
            }

            append recordStatics "$recordAttrName.setValueGetter(new ValueGetter() {\n"
            append recordStatics "public double getValue(Record record, RecordField field, VisitInfo visitInfo) {\n"
            if {$A(-valuegetter) != ""} {
                append recordStatics " $A(-valuegetter)\n"
            } else {
                append recordStatics "     return (double) (($class)record).$getter;\n"
            }
            append recordStatics "}\n"
            append recordStatics "public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {\n"
            if {$A(-valuegetter) != ""} {
                append recordStatics "     return \"\"+ getValue(record, field, visitInfo);\n"

            } else {
                append recordStatics "     return \"\"+ (($class)record).$getter;\n"
            }
            append recordStatics "}\n"
            append recordStatics "});\n"
        }

        if {$A(-searchsuffix)!=""} {
            append recordStatics "${recordAttrName}.setProperty(RecordField.PROP_SEARCH_SUFFIX,\"$A(-searchsuffix)\");\n"
        }
        if {$A(-bitfields)!=""} {
            append recordStatics "${recordAttrName}.setProperty(RecordField.PROP_BITFIELDS,\"$A(-bitfields)\");\n"
        }
        if {$A(-enums)!=""} {
            append recordStatics "List<String\[\]> ${var}_enums = new ArrayList<String\[\]>();\n"
            foreach {value label} $A(-enums) {
                append recordStatics  "${var}_enums.add(new String\[\]{\"$value\",\"$label\"});\n"
            }
            append recordStatics "${recordAttrName}.setEnumeratedValues(${var}_enums);\n"
        }


        incr recordSize $typeSize
        incr quickScanSize $numBytes
    }
    append csv $csvPost
    append csv "return myCnt+superCnt;\n"
    append csvHeader $csvHeaderPost
    append csvHeader "return myCnt+superCnt;\n"
    append statics "public static final int ATTR_LAST = ATTR_FIRST + $varCnt;\n"
    
    append getvalue "return super.getValue(attrId);\n"

    puts $fp [indent $statics]
    append recordStatics "\n\}\n"
    puts $fp [indent $recordStatics]
    puts $fp [indent $decl]
    puts $fp [method "public  ${class}(${class} that)" "super(that);\n$ctor3\n$A(-extraCopyCtor)"]
    puts $fp [method "public  ${class}(RecordFile file)" "super(file);"]
    
    puts $fp [method "public  ${class}(RecordFile file, boolean bigEndian)" "super(file, bigEndian);"]
    puts $fp [method "public int getLastAttribute()" "return ATTR_LAST;"]


    puts $fp [method "public  boolean equals(Object object)" "if(!super.equals(object)) {System.err.println(\"bad super\"); return false;} if(!(object instanceof $class)) return false;\n${class} that = (${class} ) object;\n$equals return true;"]

    if {0} {
        #Don't write out the test code
        puts $fp [method "public  void makeRandom()" "super.makeRandom();\n$testcode"]
    }

    puts $fp $A(-extraBody)

    puts $fp [method "protected void addFields(List<RecordField> fields)" "super.addFields(fields);\nfields.addAll(FIELDS);"]


    puts $fp [method "public double getValue(int attrId)" $getvalue]

    if {$extraRecordSize==""} {
        puts $fp [method "public int getRecordSize()" "return super.getRecordSize() + $recordSize;"]
    } else {
        puts $fp [method "public int getRecordSize()" "return super.getRecordSize() + $recordSize + [join $extraRecordSize +];"]
    }
    set extraRead ""
    set extraWrite ""
    append readCode $A(-readPost);
    append writes $A(-writePost)
    if {$A(-lineoriented)} {
        append writes "printWriter.print(\"\\n\");\n"
        puts $fp [method "public ReadStatus read(RecordIO recordIO) throws Exception" "ReadStatus status = ReadStatus.OK; \nString line = recordIO.readLine();\nif(line == null) return ReadStatus.EOF;\nline = line.trim();\nif(line.length()==0) return status;\n$extraRead[readHeader][indent $readCode]\nreturn status;"]
        puts $fp [method "public void write(RecordIO recordIO) throws IOException" "String delimiter = \"$delimiterString\";\nPrintWriter  printWriter= recordIO.getPrintWriter();\n${extraWrite}${writes}"]
    } else {
        if {!$baseClass} {
            set extraRead "ReadStatus status= super.read(recordIO);\nif(status!=ReadStatus.OK)  return status;\n"
            set extraWrite "super.write(recordIO);\n"
        }
        puts $fp [method "public ReadStatus read(RecordIO recordIO) throws Exception" "DataInputStream dis = recordIO.getDataInputStream();\n$extraRead[readHeader][indent $readCode]\nreturn ReadStatus.OK;"]
        puts $fp [method "public void write(RecordIO recordIO) throws IOException" "DataOutputStream dos = recordIO.getDataOutputStream();\n${extraWrite}${writes}"]
    }






    puts $fp [method "public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)" $csv]
    puts $fp [method "public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)" $csvHeader]
    puts $fp [method "public void print(Appendable buff)" "${print}" " throws Exception "]

    puts $fp  $getters

    if {$A(-makefile)} {
        puts $fp  "\}\n"
    }


    puts $fp  "\}\n\n\n"
    close $fp
}

proc dqt {s} {
    return "\"$s\""
}

proc method {sig body {extra ""}} {
    set sig [indent "$sig $extra \{"]                       
    return "$sig[indent2 $body][indent \}]\n\n"
}


proc cleanUpVar {} {
    return 0
}

proc getDefaultValue {} {
    return "";
}

proc indent2 {s} {
    return [indent $s "        "]
}

proc indent {s {space {    }}} {
    set result ""
    foreach line [split $s "\n"] {
        set line [string trim $line]
        append result $space
        append result $line
        append result "\n"
    }
    set result
}

proc getTypeSize {type} {
    switch $type {
        ubyte {return 1}
        byte {return 1}
        char {return 1}

        short {return 2}
        ushort {return 2}

        int {return 4}
        uint {return 4}

        float {return 4}

        long {return 8}
        double {return 8}
    }
    error "Unknown type $type"
}

proc extraReadForVar {var type} {
    return ""
}

proc extraImport {} {
    return ""
}

proc readHeader {} {
    return ""
}



