source ../record/generate.tcl




generateRecordClass org.ramadda.data.services.PointDataRecord  -fields   {
    {Latitude    double}
    {Longitude    double}
    {Altitude    double}
    {Time    long}
    {Dvals    {double[getDvalsSize()]}}
    {Svals    {String[getSvalsSize()]}}
}  -extraBody {
    public int dvalsSize;
    public int svalsSize;
    public int getDvalsSize() {
        return dvalsSize;
    }
    public int getSvalsSize() {
        return svalsSize;
    }
}

