
##
## Note: the lib.tcl is generated during the build process from lib.tcl.template
##

gen::setDoTclEvaluation 1
gen::setTargetDir ../htdocs/userguide
gen::defineMacro {<%ramadda.version%>} {17.139.0}


namespace eval wiki {}

proc wiki::raw {t} {
     return $t
}

proc wiki::clean {t} {
     regsub -all "\{\{" $t "\{<noop>\{" t
     set t
}


proc wiki::tagdef {t {attrs {}}} {
    set block  ""
    set attrs [string trim $attrs]
    if {$attrs==""} {
        set   block  "{{$t}}"
    } else {
        set block   "{{$t <i>$attrs</i>}}"
    }
    set block [wiki::text $block]
    set t "<a name=\"$t\"></a>Tag: <a href=\"#${t}\">$t</a><div class='ramadda-wiki-tag'>$block</div>"
    return [wiki::clean $t]
}

proc wiki::tagdefBlock {t {attrs {}}} {
    set block  ""
    set attrs [string trim $attrs]
    if {$attrs==""} {
        set   block  [wiki::text  "{{$t}}"]
    } else {
        set block   [wiki::text  "{{$t $attrs}}"]
    }
    return [wiki::clean "<a name=\"$t\"></a>$block"]
}

proc wiki::tag {t {attrs {}}} {
    if {$attrs==""} {
        set t "{{$t}}"
    } else {
         set t "{{$t <i>$attrs</i>}}"
    }
    return [wiki::clean  $t]
}

proc wiki::text {t} {
if {$::doXml} {
    set t [string trim $t]
    set t "\n+pre class=wikitext\n$t\n-pre\n"
} else {
  set t "<pre>$t</pre>"
}
    return [wiki::clean  $t]
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

