
## Definition of the LVIS record structures

##Load in the generate library
source ../../../data/record/generate.tcl


##Overwrite a couple of the hooks to add some code


## override procedure to add in extra import statements in the generated code
proc extraImport {} {
    return "import org.ramadda.data.point.Waveform;\nimport ucar.unidata.util.Misc;\n"
}



##This gets called when creating the Record.read method.
##var is the name of the variable
##we add in calls to setLatiude/setLongitude/setAltitude
##after reading the value 
proc extraReadForVar {var type} {
    if {[regexp {tlat|glat|lat0} $var]} {
        return "setLatitude($var);\n"
    }  elseif {[regexp {tlon|glon|lon0} $var]} {
        return  "setLongitude(org.ramadda.util.GeoUtils.normalizeLongitude($var));\n"
    } elseif {[regexp {zt|zg|z431|z527} $var]} {
        return  "setAltitude($var);\n"
    }
    return ""
}



##define the records
##We use -declare 0 for the time field because this member is defined in the LvisRecord base class
##We use the   -extraBody  argument to add extra code to the generated class
##The  getWaveform(String name) methods we are adding are used to get the named waveform
## The LvisFile.getWaveformNames method is what returns the names of the waveforms


generateRecordClass org.ramadda.geodata.lidar.lvis.LceRecordV1_2  -super LvisRecord   -fields  { 
    {lfid int}
    {shotnumber int}
    {lvisTime double -declare 0}
    {tlon double}
    {tlat double}
    {zt float}
} 



generateRecordClass org.ramadda.geodata.lidar.lvis.LceRecordV1_3  -super LvisRecord   -fields { 
    {beginquickscan}
    {lfid int}
    {shotnumber int}
    {azimuth float}
    {incidentangle float}
    {range  float}
    {endquickscan}
    {lvisTime double -declare 0}
    {tlon double}
    {tlat double}
    {zt float}
} 


generateRecordClass org.ramadda.geodata.lidar.lvis.LgeRecordV1_2  -super LvisRecord   -fields {
    {beginquickscan}
    {lfid int}
    {shotnumber int}
    {lvisTime double -declare 0}
    {endquickscan}
    {glon double}
    {glat double}
    {zg float}
    {beginquickscan}
    {rh25 float}
    {rh50 float}
    {rh75 float}
    {rh100 float}
    {endquickscan}
}     -extraBody {
    public float[] getAltitudes() {
        return new float[]{zg,zg+rh25,zg+rh50,zg+rh75,zg+rh100};
    }
}

generateRecordClass org.ramadda.geodata.lidar.lvis.LgeRecordV1_3 -super LvisRecord   -fields {
    {beginquickscan}
    {lfid int}
    {shotnumber int}
    {azimuth float}
    {incidentangle float}
    {range float}
    {endquickscan}
    {lvisTime double -declare 0}
    {glon double}
    {glat double}
    {zg float}
    {beginquickscan}
    {rh25 float}
    {rh50 float}
    {rh75 float}
    {rh100 float}
    {endquickscan}
}    -extraBody {
    public float[] getAltitudes() {
        return new float[]{zg,zg+rh25,zg+rh50,zg+rh75,zg+rh100};
    }
}


generateRecordClass org.ramadda.geodata.lidar.lvis.LgwRecordV1_2  -super LvisRecord   -fields {
    {beginquickscan}
    {lfid int}
    {shotnumber int}
    {endquickscan}
    {lvisTime double -declare 0}
    {lon0 double -label {Lon. waveform top} -desc {Longitude of the highest sample of the waveform }}
    {lat0 double -label {Lat. waveform top} -desc {Latitude of the highest sample of the waveform }}
    {z0 float -chartable true -searchable true -unit m -label {Waveform top} -desc {Elevation of the highest sample of the waveform}}
    {beginquickscan}
    {lon431 double -label {Lon. waveform bottom} -desc {Longitude of the lowest sample of the waveform }}
    {lat431 double -label {Lat. waveform bottom} -desc {Latitude of the lowest sample of the waveform }}
    {z431 float -chartable true -searchable true -unit m  -label {Waveform bottom} -desc {Elevation of the lowest sample of the waveform }}
    {sigmean float -chartable true -searchable true -label {Signal Mean}}
    {returnWaveform {ubyte[432]} -label {Return waveform}}
    {endquickscan}
}   -extraBody {
    public Waveform getWaveform(String name) {
        return new Waveform(toFloat(returnWaveform), new float[]{0,255},sigmean, z0,z431,lat0,lon0,lat431,lon431);
    }
}


generateRecordClass org.ramadda.geodata.lidar.lvis.LgwRecordV1_3  -super LvisRecord   -fields {
    {beginquickscan}
    {lfid int}
    {shotnumber int}
    {azimuth float}
    {incidentangle float}
    {range float}
    {endquickscan}
    {lvisTime double -declare 0}
    {lon0 double -label {Lon. waveform top} -desc {Longitude of the highest sample of the waveform }}
    {lat0 double -label {Lat. waveform top} -desc {Latitude of the highest sample of the waveform }}
    {z0 float -chartable true -searchable true -unit m -label {Waveform top} -desc {Elevation of the highest sample of the waveform}}
    {lon431 double -label {Lon. waveform bottom} -desc {Longitude of the lowest sample of the waveform}}
    {lat431 double -label {Lat. waveform bottom} -desc {Latitude of the lowest sample of the waveform}}
    {z431 float -chartable true -searchable true -unit m  -label  {Waveform bottom} -desc {Elevation of the lowest sample of the waveform }}
    {sigmean float  -chartable true -searchable true -label {Signal Mean}}
    {beginquickscan}
    {txwave {ubyte[80]} -label {Transmit waveform}}
    {rxwave {ubyte[432]} -label {Return waveform}}
    {endquickscan}
}  -extraBody {
    public Waveform getWaveform(String name) {
        if(name == null|| name.length()==0 || name.equals(WAVEFORM_RETURN)) {
            return new Waveform(toFloat(rxwave), new float[]{0,255},sigmean, z0,z431,lat0,lon0,lat431,lon431);
        }
        return new Waveform(toFloat(txwave), new float[]{0,255},0);
    }
}



generateRecordClass org.ramadda.geodata.lidar.lvis.LgwRecordV1_4  -super LvisRecord   -fields {
    {beginquickscan}
    {lfid int}
    {shotnumber int}
    {azimuth float}
    {incidentangle float}
    {range float}
    {endquickscan}
    {lvisTime double -declare 0}
    {lon0 double -label {Lon. waveform top} -desc {Longitude of the highest sample of the waveform }}
    {lat0 double -label {Lat. waveform top} -desc {Latitude of the highest sample of the waveform }}
    {z0 float -chartable true -searchable true -unit m -label {Waveform top} -desc {Elevation of the highest sample of the waveform}}
    {lon527 double -label {Lon. waveform bottom} -desc {Longitude of the lowest sample of the waveform}}
    {lat527 double -label {Lat. waveform bottom} -desc {Latitude of the lowest sample of the waveform}}
    {z527 float -chartable true -searchable true -unit m  -label  {Waveform bottom} -desc {Elevation of the lowest sample of the waveform }}
    {sigmean float  -chartable true -searchable true -label {Signal Mean}}
    {beginquickscan}
    {txwave {ubyte[240]} -label {Transmit waveform}}
    {rxwave {ubyte[1056]} -label {Return waveform}}
    {endquickscan}
}  -extraBody {
    public Waveform getWaveform(String name) {
        if(name == null|| name.length()==0 || name.equals(WAVEFORM_RETURN)) {
            return new Waveform(toFloat(rxwave), new float[]{0,255},sigmean, z0,z527,lat0,lon0,lat527,lon527);
        }
        return new Waveform(toFloat(txwave), new float[]{0,255},0);
    }
}





generateRecordClass org.ramadda.geodata.lidar.lvis.LvisTextRecord -super org.ramadda.geodata.lidar.lvis.LvisRecord  -lineoriented 1 -delimiter { +} -fields  { 
{lfid int}
{SHOTNUMBER double}
{lvisTime double -declare 0}
{LONGITUDE_CENTROID double}
{LATITUDE_CENTROID double}
{ELEVATION_CENTROID double}
{LONGITUDE_LOW double}
{LATITUDE_LOW double}
{ELEVATION_LOW double}
{LONGITUDE_HIGH double}
{LATITUDE_HIGH double}
{ELEVATION_HIGH double}
}  -extraBody {
    public double getLatitude() {
        return LATITUDE_CENTROID;
    }
    public double getLongitude() {
        return org.ramadda.util.GeoUtils.normalizeLongitude(LONGITUDE_CENTROID);
    }
    public double getAltitude() {
        return ELEVATION_CENTROID;
    }

}
