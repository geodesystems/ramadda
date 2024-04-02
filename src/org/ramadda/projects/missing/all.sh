process() {
    cd $1
    echo "processing $1"
    sh ../makecases.sh data.html> entries.xml
    jar -cvf "../missing$1.zip" entries.xml images/* >/dev/null
    cd ..
}

process data1
process data2
process data3
process data4
process data5
process data6
process data7
process data8
process data9

