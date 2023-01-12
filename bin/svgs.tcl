#Converts svg patterns from https://github.com/iros/patternfills
#into the JS that is used in Openlayers
puts "let patterns = {";
foreach file [glob */*.svg] {
    set c [read [open $file]]
    regsub  {.*/} $file {} file
    regsub {\.svg} $file {} id
    regsub -all {\n} $c { } c
    regsub -all {  +} $c { } c
    regsub -all {> +<} $c {><} c        
    regsub -all {"} $c {'} c    
    regexp {width='(.*?)'} $c {} width
    regexp {height='(.*?)'} $c {} height    
    puts "\"$id\":{width:$width,height:$height,svg:\"$c\"},"
}
puts "}"
