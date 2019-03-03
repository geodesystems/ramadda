source ../../../data/record/generate.tcl


## tell the script to take the var names and clean them up, removing whitespace, etc
proc cleanUpVar {} {
    return 1
}




generateRecordClass org.ramadda.geodata.lidar.geotiff.GeoKeyHeader -fields {
    {{Key Directory Version} ushort}
    {{Key Revision} ushort}
    {{Minor Revision} ushort}
    {{Number Of Keys} ushort}
}

generateRecordClass org.ramadda.geodata.lidar.geotiff.GeoKey -fields {
    {{Key ID} ushort}
    {{TIFF Tag Location} ushort}
    {Count ushort}
    {{Value Offset} ushort}
} -extraBody {

    private byte[]geoKeyAsciiParams;
    private double[]doubles;

    private String stringValue;
    public GeoKey(RecordFile file, boolean endian,byte[]geoKeyAsciiParams,double[]doubles) {
        super(file, endian);
        this.geoKeyAsciiParams = geoKeyAsciiParams;
        this.doubles = doubles;
    }



    public double getValue() {
        if(tiffTagLocation==0) {
            return (double) getValueOffset();
        }
        return doubles[getValueOffset()];
        
    }


    public String getStringValue() {
        if(stringValue == null) {
            if(tiffTagLocation==34737) {
                stringValue = new String(geoKeyAsciiParams, getValueOffset(), getCount());
            } else  if(tiffTagLocation==34736) {
                stringValue = ""+getValue();
            } else {
                stringValue = ""+getValueOffset();
            }
        }
        return stringValue;
    }


}


generateRecordClass org.ramadda.geodata.lidar.geotiff.GeoKeyDouble -fields {
    {{value} double}
}
