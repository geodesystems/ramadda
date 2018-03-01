


source ../record/generate.tcl

##Overwrite a couple of the hooks to add some code


proc extraInit {var type} {
    if {[regexp {latitude} $var]} {
        return "setLatitude($var);\n"
    }  elseif {[regexp {longitude} $var]} {
        return  "setLongitude($var-360);\n"
    } elseif {[regexp {altitude} $var]} {
        return  "setAltitude($var);\n"
    }
    return ""
}



generateRecordClass org.ramadda.data.point.LatLonPointRecord -super org.ramadda.data.record.GeoRecord -fields  { 
    { latitude double}
    { longitude double}
    { altitude double}
    { values double[getNumberOfValues()]}
}  -extraCopyCtor {this.numberOfValues = that.numberOfValues;}  -extraBody {
    
    private int numberOfValues;

//    public LatLonPointRecord(int numberOfValues) {
//	super(true);
//        this.numberOfValues = numberOfValues;
//        values = new double[getNumberOfValues()];
//    }

//    public LatLonPointRecord(boolean bigEndian, int numberOfValues) {
//        super(bigEndian);
//        this.numberOfValues = numberOfValues;
//        values = new double[getNumberOfValues()];
//    }

    public int getNumberOfValues() {
        return numberOfValues;
    }

    public void setNumberOfValues(int v) {
        numberOfValues = v;
    }


}

