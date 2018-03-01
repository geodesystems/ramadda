


source ../../record/generate.tcl

generateRecordClass org.ramadda.data.point.binary.DoubleLatLonAltRecord  -super org.ramadda.data.point.PointRecord   -fields  { 
    { latitude double}
    { longitude double}
    { altitude double}
} 


generateRecordClass org.ramadda.data.point.binary.DoubleLatLonRecord  -super org.ramadda.data.point.PointRecord   -fields  { 
    { latitude double}
    { longitude double}
} 


generateRecordClass org.ramadda.data.point.binary.DoubleLatLonAltIntensityRecord  -super org.ramadda.data.point.PointRecord   -fields  { 
    { latitude double}
    { longitude double}
    { altitude double}
    { intensity double}
} 

generateRecordClass org.ramadda.data.point.binary.FloatLatLonAltRecord  -super org.ramadda.data.point.PointRecord   -fields  { 
    { lat float}
    { lon float}
    { alt float}
}  -extraBody {
    public double getLatitude() {
        return (double) lat;
    }
    public double getLongitude() {
        return (double) lon;
    }
    public double getAltitude() {
        return (double) alt;
    }
}


generateRecordClass org.ramadda.data.point.binary.FloatLatLonRecord  -super org.ramadda.data.point.PointRecord   -fields  { 
    { lat float}
    { lon float}
}  -extraBody {
    public double getLatitude() {
        return (double) lat;
    }
    public double getLongitude() {
        return (double) lon;
    }
}
