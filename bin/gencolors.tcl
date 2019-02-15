
set colors {
dimgray:#696d7d:#fff
metal:#6f9283
artichoke:#8d9f87
darkvanilla:#cdc6a5
almond:#f0dcca
tumbleweed:#e1b07e
palegold:#e5be9e
turquoise:#a7cdbd
vermillion:#d56f3e
bone:#e2dbbe
platinum:#dfe0e2
olivine:#9cb380
yellow:#fffeec
snow
rose:mistyrose
mint:mintcream
floral:floralwhite
azure
gold:#fafad2
pink:lavenderblush
blue:ghostwhite:#f1f1f1
babypowder:#fcfff7
cornflower:#188fa7
whitesmoke:#edf7f6
cambridgeblue:#9ec1a3
white:white:#f1f1f1
gray:#F8F8F8
green:honeydew
plain:inherit
}

set classes {blurb note background block heading box   callout section}

set out [open wikicolors.txt w]


foreach c $colors {
    set tuple [split $c :]
    set name [lindex $tuple 0]
    set border ""
    if {[llength $tuple]>1} {
        set color [lindex $tuple 1]
    } else {
        set color $name
    }
    if {[llength $tuple]>2} {
        set border [lindex $tuple 2]
    }
    set css "";
    foreach class  $classes {
        if {$css !=""} {
            append css ",";
        }
        append css " .ramadda-$class-$name "
    }
    append css " {\n"
    append css "   background-color: $color;\n"
    if {$border!=""} {
        append css "   border: 1px $border solid;\n"
    }
    append css "}\n"
    puts $css
    puts "\n"
}


puts $out "+section title={{name}}"
foreach class  $classes {
    if {$class=="background"} continue;
    if {$class=="section"} continue;
    puts $out ":heading Tag: $class"
    foreach c $colors {
        set tuple [split $c :]
        set name [lindex $tuple 0]
        puts $out "+$class-$name"
        puts $out "$class-$name"
        puts $out "-$class-$name"
    }
    puts $out "----"
}
puts $out "-section"

close $out
if {0} {
:heading Tag: heading
:heading-yellow heading-yellow
:heading-snow heading-snow
:heading-rose heading-rose
:heading-mint heading-mint
:heading-floral heading-floral
:heading-azure heading-azure
:heading-turquoise heading-turquoise
:heading-gold heading-gold
:heading-pink heading-pink
:heading-blue heading-blue
:heading-white heading-white
:heading-gray heading-gray
:heading-green heading-green
:heading-plain heading-plain
}
