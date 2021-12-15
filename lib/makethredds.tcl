
file delete -force thredds
file mkdir thredds
cd thredds
puts "Unjarring thredds.war"
exec jar -xvf ../thredds.war

set skips [list .*log4.* jfree  jdom jcommon unidatacommon slf4j 52 json waterml c3p0 aws-java je-4 Saxon jackson jna- xmlbeans httpmime Hikari SGT Saxon aws c3p0 commons-fileupload commons-codec commons-io commons-lang coverity-escapers d4 failureaccess hibernate-validator jackson jaxen jboss jcip jcl  jmespath jsi- json quartz  taglibs validation waterml xmlbeans spring-webmvc checker-qual]

cd WEB-INF/classes

if {1} {
foreach jar [glob ../lib/*.jar] {
    set shouldSkip 0
    foreach skip $skips {
        if {[regexp $skip $jar]} {
            set shouldSkip 1
            break;
        }
    }
    if {$shouldSkip} {
        puts "skipping $jar"
        continue
    }

#    if {[regexp netcdf $jar]} {
#        puts "skipping $jar"
#        continue
#    }


    puts "Unjarring $jar"
    exec jar -xvf $jar
    if {[file exists LICENSE]} {
        puts "Deleting LICENSE"
        exec rm -r -f LICENSE
    }
}

##unjar the common jar
#puts "Unjarring unidatacommon.jar"
#exec jar -xvf ../../../unidatacommon.jar

#puts "Unjarring ncIdv.jar"
#exec jar -xvf ../../../ncIdv.jar

file delete -force org/apache/log4j
file delete -force org/slf4j


puts "Making ramaddatds.jar"
puts "pwd: [pwd]"
puts "exec: [exec pwd]"
set files ""
foreach file [glob *] {
    if {$file=="visad"} continue
    append files " "
    append files "\{[file tail $file]\}"
#    puts "$file"
}
}


##for good measure
file delete -force org/apache/logging/log4j/core/lookup/JndiLookup.class
file delete -force META-INF/MANIFEST.MF
cd ../../META-INF
file delete -force MANIFEST.MF

cd ../WEB-INF/classes
puts [exec pwd]


set execLine "jar -Mcvf ../../../ramaddatds.jar $files"
eval exec $execLine


