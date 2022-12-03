#
#The com.monitorjbl.xlsx.StreamingReader for streaming XLS files 
#depends on an older version of tika so for now the ramaddatika.jar
#is from (I think):
#https://mvnrepository.com/artifact/org.apache.tika/tika-app/2.5.0
#In the future if this gets resolved we can use the a more current version of tika from:
#https://tika.apache.org/download.html
#
#Because github doesn't like large files the tika-app.jar is no longer in github
#So, to run this script that produces the ramaddatika.jar
#you need to download the tika-app jar
#
file delete -force tika
file mkdir tika
cd tika
puts "unzipping tika"
set rc [catch {exec unzip -o ../tika-app-2.5.0.jar} msg]

set delete {
    com/google dods ucar thredds org/slf4j org/apache/logging org/apache/http  org/joda
    log4j2.xml
    log4j2_batch_process.properties
    META-INF/org/apache/logging/log4j
    META-INF/maven/org.apache.logging.log4j
    META-INF/maven/org.apache.logging.log4j/log4j-api
    META-INF/maven/org.apache.logging.log4j/log4j-core
    META-INF/maven/org.apache.logging.log4j/log4j-slf4j-impl
    META-INF/versions/9/org/apache/logging/log4j
    META-INF/services/org.apache.logging.log4j.message.ThreadDumpMessage$ThreadInfoFactory
    META-INF/services/org.apache.logging.log4j.spi.Provider
    META-INF/services/org.apache.logging.log4j.core.util.ContextDataProvider
    META-INF/services/org.apache.logging.log4j.util.PropertySource
    ./pipes-fork-server-default-log4j2.xml
}

foreach del $delete {
    puts "deleting $del"
    file delete -force $del
}




#nuke any remaining log4j thing
#exec {rm -r `find . -name "*log4j*"`}

puts "Making ramaddatika.jar"
puts "pwd: [pwd]"
exec jar -cvf ../ramaddatika.jar .



