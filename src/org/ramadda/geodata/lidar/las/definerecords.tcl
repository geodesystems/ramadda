source ../../../data/record/generate.tcl


## tell the script to take the var names and clean them up, removing whitespace, etc
proc cleanUpVar {} {
    return 1
}

##add in an extra import line
proc extraImport {} {
    return "import org.ramadda.geodata.lidar.*;\n"
}

set globalEncodingGetters ""
append globalEncodingGetters [bitMask GPSTimeType {Global Encoding} 0]
append globalEncodingGetters [bitMask WaveFormDataPacketsInternal  {Global Encoding} 1]
append globalEncodingGetters [bitMask WaveFormDataPacketsExternal  {Global Encoding} 2]
append globalEncodingGetters [bitMask SyntheticReturnNumbers  {Global Encoding} 3]





generateRecordClass org.ramadda.geodata.lidar.las.LasHeader  

generateRecordClass org.ramadda.geodata.lidar.las.Test  -fields   {
    {{File Source ID}    ushort -default 1}
    {{Number of points by return} {uint[5]}}
    {{Min Z} double}} 



generateRecordClass org.ramadda.geodata.lidar.las.LasHeader_V12 -super LasHeader -extraBody $globalEncodingGetters  -fields   {
    {{File Signature} {string[4]} -default "LASF"}
    {{File Source ID}    ushort -default 1}
    {{Global Encoding}  ushort}
    {{Project ID - GUID data 1}  uint}
    {{Project ID - GUID data 2}  ushort}
    {{Project ID - GUID data 3}  ushort}
    {{Project ID - GUID data 4}  {string[8]}}
    {{Version Major}  byte -default  1}
    {{Version Minor} byte -default 2}
    {{System Identifier} {string[32]}}
    {{Generating Software} {string[32]}}
    {{File Creation Day of Year} ushort}
    {{File Creation Year}  ushort}
    {{Header Size} ushort}
    {{Offset to point data}  uint}
    {{Number of Variable Length Records} uint}
    {{Point Data Format ID}  byte}
    {{Point Data Record Length}  ushort}
    {{Number of point records} uint}
    {{Number of points by return} {uint[5]}}
    {{X scale factor} double -default 1.0}
    {{Y scale factor} double -default 1.0}
    {{Z scale factor} double -default 1.0}
    {{X offset} double -default 0}
    {{Y offset} double -default 0}
    {{Z offset} double -default 0}
    {{Max X} double}
    {{Min X} double}
    {{Max Y} double}
    {{Min Y} double}
    {{Max Z} double}
    {{Min Z} double}} 


generateRecordClass org.ramadda.geodata.lidar.las.LasHeader_V13 -super LasHeader_V12 -fields {
    {{Start of Waveform Data Packet Record} long}
}   


##TODO: read the buffer
generateRecordClass org.ramadda.geodata.lidar.las.LasVariableLengthRecord  -fields {
    {Reserved ushort}
    {{User ID}      {string[16]}}
    {{Record ID}  ushort}
    {{Record Length After Header}  ushort}
    {{Description} {string[32]}}} -extraBody {
        byte[]buffer;
        public void setBuffer(byte[]buffer) {
            this.buffer  = buffer;
        }
    } -writePost {
        write(dos, this.buffer);
    }  



generateRecordClass org.ramadda.geodata.lidar.las.LasExtendedVariableLengthRecord  -fields {
    {Reserved short}
    {{User ID}      {string[16]}}
    {{Record ID}  short}
    {{Record Length After Header}  short}
    {{Description} {string[32]}}
}


generateRecordClass org.ramadda.geodata.lidar.las.WaveFormDescriptor  -fields {
    {{Bits per sample} byte}
    {{Waveform compression type} byte}
    {{Number of samples} uint}
    {{Temporal Sample Spacing} uint}
    {{Digitizer Gain} double}
    {{Digitizer Offset} double}
}



generateRecordClass org.ramadda.geodata.lidar.las.BasePointRecord  -super LasPointRecord




generateRecordClass org.ramadda.geodata.lidar.las.PointRecord0  -super BasePointRecord -fields {
    {X  int -csv {pw.print(getLongitude());} -valuegetter {LasPointRecord lasRecord = ((LasPointRecord)record);if(visitInfo.getProperty("georeference",false)) return lasRecord.getLongitude();return lasRecord.getScaledAndOffsetX();} }
    {Y  int -csv {pw.print(getLatitude());} -valuegetter {LasPointRecord lasRecord = ((LasPointRecord)record);if(visitInfo.getProperty("georeference",false)) return lasRecord.getLatitude();return lasRecord.getScaledAndOffsetY();}}
    {Z  int -csv {pw.print(getAltitude());} -valuegetter {LasPointRecord lasRecord = ((LasPointRecord)record);if(visitInfo.getProperty("georeference",false)) return lasRecord.getAltitude();return lasRecord.getScaledAndOffsetZ();}}
    {Intensity ushort -label "Intensity" -searchable true -searchsuffix {Pulse return magnitude} -chartable true}
    {{Return and flags}  byte}
    {{Classification Bit Field} byte   }
    {{Classification} int  -label {Classification}  -searchable true -synthetic 1 -getter getClassification  -enums {0 {Created, never classified} 1 {Unclassified} 2 Ground 3 {Low Vegetation} 4 {Medium Vegetation} 5 {High Vegetation} 6 Building 7 {Low Point (noise)} 8 {Model Key-point (mass point)} 9 Water 10 {10}  11 11  12 {Overlap Points} 13 13 14 14 15 15 16 16}}
    {{Synthetic} int  -label {Synthetic} -searchable true -synthetic 1 -getter getSynthetic  -enums {0 {Not Synthetic} 1 {Synthetic}}}
    {{Scan Angle Rank} byte}
    {{User Data} byte}
    {{Point Source ID} ushort}
}  -extraBody {
    int cnt = 0;
    public void recontextualize(LidarFile lidarFile) {
        super.recontextualize(lidarFile);
        LasFile newLasFile = (LasFile) lidarFile;
        newLasFile.importRecord(this);
    }
    public int getClassification() {
        int c =(int) (classificationBitField & 0x001F);
        return c;
    }

    public int getSynthetic() {
        if((classificationBitField & (1<<5))==0) return 0;
        return 1;
    }

    public void setReturnNumber(byte b) {
        int masked = mask(returnAndFlags,0,2,true);
        returnAndFlags = (byte)(masked & mask(b,3,7,true));
    }
    public void setNumberOfReturns(byte b) {
        int masked = mask(returnAndFlags,3,5,true);
        returnAndFlags = (byte)(masked | mask(b,3,7,false));
    }
} -readPost {
    ((LasFile)getRecordFile()).initRecord(this);
}

generateRecordClass org.ramadda.geodata.lidar.las.PointRecord1 -super PointRecord0 -fields  {
    {{GPS Time} double}
} -extraBody {
    public long getLidarTime() {
        return ((LasFile)getRecordFile()).convertTime(this,gpsTime);
    }
    public boolean hasLidarTime() {
        return true;
    }
}

generateRecordClass org.ramadda.geodata.lidar.las.PointRecord2  -super PointRecord0 -fields {
    {{red} ushort}
    {{green} ushort}
    {{blue} ushort}
}  -extraBody {
    public short[] getRgb() {
        return new short[]{(short)red,(short)green,(short)blue};
    }
    public void setRgb(short[]rgb) {
        this.red = rgb[0];        
        this.green = rgb[1];        
        this.blue = rgb[2];        
    }
} 


generateRecordClass org.ramadda.geodata.lidar.las.PointRecord3  -super PointRecord0 -fields {
    {{GPS Time} double}
    {{red} ushort}
    {{green} ushort}
    {{blue} ushort}
} -extraBody {
    public short[] getRgb() {
        return new short[]{(short)red,(short)green,(short)blue};
    }
    public void setRgb(short[]rgb) {
        this.red = rgb[0];        
        this.green = rgb[1];        
        this.blue = rgb[2];        
    }

//    public void setLidarTime(double time) {
//        this.gpsTime = time;
//    }
    public long getLidarTime() {
        return ((LasFile)getRecordFile()).convertTime(this,gpsTime);
    }
    public boolean hasLidarTime() {
        return true;
    }
} 


generateRecordClass org.ramadda.geodata.lidar.las.PointRecord4  -super PointRecord1 -fields {
    {{Wave Packet Descriptor Index} byte}
    {{Byte offset to waveform data} long}
    {{Waveform packet size in bytes} int}
    {{Return Point Waveform Location} float}
    {{Xt} float}
    {{Yt} float}
    {{Zt} float}
} 



generateRecordClass org.ramadda.geodata.lidar.las.PointRecord5 -super PointRecord3 -fields {
    {{Wave Packet Descriptor Index} byte}
    {{Byte offset to waveform data} long}
    {{Waveform packet size in bytes} uint}
    {{Return Point Waveform Location} float}
    {{Xt} float}
    {{Yt} float}
    {{Zt} float}
} 



