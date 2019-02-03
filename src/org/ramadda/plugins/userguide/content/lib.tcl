
##
## Note: the lib.tcl is generated during the build process from lib.tcl.template
##

gen::setDoTclEvaluation 1
gen::setTargetDir ../htdocs/userguide
gen::defineMacro {<%ramadda.version%>} {2.3}


namespace eval wiki {}

proc wiki::tagdef {t {attrs {}}} {
    set block  ""
    set attrs [string trim $attrs]
    if {$attrs==""} {
        set   block  "{{$t}}"
    } else {
        set block   "{{$t <i>$attrs</i>}}"
    }
    return "<a name=\"$t\"></a>$block"
}

proc wiki::tagdefBlock {t {attrs {}}} {
    set block  ""
    set attrs [string trim $attrs]
    if {$attrs==""} {
        set   block  [ht::pre "{{$t}}"]
    } else {
        set block   [ht::pre "{{$t $attrs}}"]
    }
    return "<a name=\"$t\"></a>$block"
}

proc wiki::tag {t {attrs {}}} {
    if {$attrs==""} {
        return "{{$t}}"
    } else {
        return "{{$t <i>$attrs</i>}}"
    }
}

proc wiki::text {t} {
    return "<div style=\"margin:10px;\"><pre>$t</pre></div>"
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



proc importxml {file} {
   lappend ::filesToCopy [file join  $file] [file join [gen::getTargetDir]  [file dirname $file]]
   set xml [import  $file]
   set href "<a href=\"$file\">$file</a>"
   set xml [xml [string trim $xml]]
   regsub {</pre>\s*</blockquote>} $xml "" xml
   append xml "\n$href"
   append xml "\n</pre></blockquote>"
   set xml
}

