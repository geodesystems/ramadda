gen::setDoTclEvaluation 1
gen::setTargetDir ../htdocs/pointdocs

gen::defineMacro {<%ramadda.version%>} {1.6b}

namespace eval wiki {}

proc wiki::tagdef {t {attrs {}}} {
    if {$attrs==""} {
        return "<a name=\"$t\"></a>{{$t}}"
    } else {
        return "<a name=\"$t\"></a>{{$t <i>$attrs</i>}}"
    }
}

proc wiki::tag {t {attrs {}}} {
    if {$attrs==""} {
        return "{{$t}}"
    } else {
        return "{{$t <i>$attrs</i>}}"
    }
}

proc wiki::text {t} {
    return "<blockquote><pre>$t</pre></blockquote>"
}

proc class {c} {
    return "<code>$c</code>"
}



proc import {file} {
   set fp [open $file r ]
   set c [read $fp]
   return $c
}


proc xml {args} {
  set xml [ht::pre [join $args " "]]
  regsub -all {(&lt;!--.*?--&gt;)} $xml {<span class="xmlcomment">\1</span>} xml
  regsub -all {(&lt;!\[CDATA\[.*?\]\]&gt;)} $xml {<span class="xmlcdata">\1</span>} xml
  regsub -all {([^\s]*)=&quot;} $xml {<span class="xmlattr">\1</span>="} xml


  foreach t [array names ::taghome] {
     regsub -all "lt;$t" $xml "lt;<a[tagref $t]" xml
     regsub -all "/$t" $xml "/<a[tagref $t]" xml
  }
  return "<blockquote>$xml</blockquote>"
}




proc copy_file {file} {
    set targetDir [file join [gen::getTargetDir]  [file dirname $file] ]
    file mkdir $targetDir
    lappend ::filesToCopy [file join  $file] $targetDir
}



proc import_xml {file} {
   copy_file $file
   set xml [import  $file]
    set href "<a href=\"$file\">[file tail $file]</a>"
   set xml [xml [string trim $xml]]
   regsub {</pre>\s*</blockquote>} $xml "" xml
##   append xml "\n$href</pre></blockquote>"
    return "<br> $href $xml"
}


proc importcsv {file {css {}}} {
    copy_file $file
   lappend ::filesToCopy [file join  $file] [file join [gen::getTargetDir]  [file dirname $file]]
   set csv [import  $file]
    set href "<a href=\"$file\">[file tail $file]</a>"
    return "<div style=\"margin-top:10px; margin-bottom:20px;margin-left:20px;\">$href <pre style=\"margin:0px;margin-top:5px;border: 1px #888 solid;max-height:150px; overflow-y:auto;max-width:1000px;overflow:auto;$css\">$csv</pre></div>\n"
}

