file delete -force tika
file mkdir tika
cd tika
puts "unzipping tika"
set rc [catch {exec unzip -o ../tika-app-2.1.0.jar} msg]

foreach del {com/google dods ucar thredds org/slf4j org/apache/logging org/apache/http  org/joda} {
    puts "deleting $del"
    file delete -force $del
}

puts "Making ramaddatika.jar"
puts "pwd: [pwd]"
exec jar -cvf ../ramaddatika.jar .



