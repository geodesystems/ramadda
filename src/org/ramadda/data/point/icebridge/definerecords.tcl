
source ../../../data/record/generate.tcl

#
# IceBridge MCoRDS L2 Ice Thickness
# ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/IRMCR2_MCORDSiceThickness_v01/
#




generateRecordClass org.ramadda.data.point.icebridge.McordsIrmcr2Record -lineoriented 1 -delimiter {,}  -skiplines 1 -makefile 1 -filesuper org.ramadda.data.point.text.TextFile -super org.ramadda.data.point.PointRecord   -capability {ACTION_MAPINCHART} -fields  { 
    { latitude double -declare 0}
    { longitude double  -declare 0}
    { time double}
    { thickness double -missing "-9999.0" -chartable true  }
    { altitude double -chartable true  -declare 0}
    { frame int}
    { bottom double -chartable true -missing "-9999.0" -unit "m"}
    { surface double -chartable true -missing "-9999.0" -unit "m"}
    { quality int -chartable true }
} 


##MCoRDS flighlines
##ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/IRMCR3_MCORDSsurfBottomMap_v01/2011_GR_NASA/2011_79N_Coarse/flightlines/
##

#LAT,LON,TIME,THICK,ELEVATION,FRAME,SURFACE,BOTTOM,QUALITY,A_SURF,A_BOTT
generateRecordClass org.ramadda.data.point.icebridge.McordsIrmcr3Record -makefile 1 -skiplines 1 -filesuper org.ramadda.data.point.text.TextFile -super org.ramadda.data.point.PointRecord  -lineoriented 1 -delimiter {,} -fields  { 
    { latitude double -declare 0}
    { longitude double -declare 0}
    { time double}
    { thickness double -chartable true}
    { altitude double -chartable true -declare 0}
    { frame double -chartable true}
    { surface double -chartable true}
    { bottom double -chartable true}
    { quality int -chartable true}
    { a_surface double -chartable true}
    { a_bottom double -chartable true}
} 




## ATM Ice
## ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/BLATM2_ATMicessn_v01/
##
generateRecordClass org.ramadda.data.point.icebridge.AtmIceSSNRecord -makefile 1 -filesuper org.ramadda.data.point.text.TextFile  -super org.ramadda.data.point.PointRecord  -lineoriented 1 -delimiter { +} -fields  { 
    { seconds double}
    { centerLatitude double}
    { centerLongitude double}
    { height double}
    { southToNorthSlope double -chartable true -unit degrees}
    { westToEastSlope double -chartable true -unit degrees}
    { rmsFit double -chartable true}
    { numberOfPointsUsed int}
    { numberOfPointsEdited int}
    { distanceFromTrajectory double -unit {m}}
    { trackIdentifier int}
}  -extraBody {
    //overwrite the getLatitude/getLongitude methods
    public double getLatitude() {
        return centerLatitude;
    }
    public double getLongitude() {
        return org.ramadda.util.GeoUtils.normalizeLongitude(centerLongitude);
    }
    public double getAltitude() {
        return height;
    }

}



if {0} {
   Relative Time (msec from start of data file)  
   Laser Spot Latitude (degrees X 1,000,000)
   Laser Spot Longitude (degrees X 1,000,000) 
   Elevation (millimeters)
   Start Pulse Signal Strength (relative) 
   Reflected Laser Signal Strength (relative) 
   Scan Azimuth (degrees X 1,000)
   Pitch (degrees X 1,000)
   Roll (degrees X 1,000)
   GPS Time packed (example: 153320100 = 15h 33m 20s 100ms)
}




#
#Pre-IceBridge ATM L1B Qfit Elevation and Return Strength
#ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/BLATM1B_ATMqfit_v01/
#
#IceBridge Narrow Swath ATM L1B Qfit Elevation and Return Strength
#ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/ILNSA1B_ATMnarrowSwathQfit_v01/
#

generateRecordClass org.ramadda.data.point.icebridge.QFit10WordRecord  -super org.ramadda.data.point.icebridge.QfitRecord  -fields  { 
    { relativeTime int -declare 0}
    { laserLatitude int -declare 0}
    { laserLongitude int -declare 0}
    { elevation int -declare 0  -unit mm}
    { startSignalStrength int }
    { reflectedSignalStrength int }
    { azimuth int -unit millidegree}
    { pitch int -unit millidegree}
    { roll int -unit millidegree}
    { gpsTime int }
}

generateRecordClass org.ramadda.data.point.icebridge.QFit12WordRecord  -super org.ramadda.data.point.icebridge.QfitRecord  -fields  { 
    { relativeTime int -declare 0}
    { laserLatitude int -declare 0}
    { laserLongitude int -declare 0}
    { elevation int -declare 0  -unit mm}
    { startSignalStrength int }
    { reflectedSignalStrength int }
    { azimuth int -unit millidegree}
    { pitch int -unit millidegree}
    { roll int -unit millidegree}
    { gpsPdop int} 
    { pulseWidth int}
    { gpsTime int }
}

generateRecordClass org.ramadda.data.point.icebridge.QFit14WordRecord  -super org.ramadda.data.point.icebridge.QfitRecord  -fields  { 
    { relativeTime int -declare 0}
    { laserLatitude int -declare 0}
    { laserLongitude int -declare 0}
    { elevation int -declare 0  -unit mm}
    { startSignalStrength int }
    { reflectedSignalStrength int }
    { azimuth int -unit millidegree}
    { pitch int -unit millidegree}
    { roll int -unit millidegree}
    { passiveSignal int}
    { passiveLatitude int}
    { passiveLongitude int}
    { passiveElevation int}
    { gpsTime int}
} 




##
##IceBridge BGM-3 Gravimeter L2 Geolocated Free Air Anomalies
##ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/IGBGM2_BGMGravGeoloc_v01/
##


generateRecordClass org.ramadda.data.point.icebridge.Igbgm2GravityV09Record -makefile 1 -filesuper org.ramadda.data.point.text.TextFile -super org.ramadda.data.point.PointRecord  -lineoriented 1 -delimiter { +}  -fields  {
{time double}
    { longitude double  }
    { latitude double}
    { aircraftHeight double -unit m -chartable true}
    { freeAirAnomalies double -unit mGal -chartable true}
}


generateRecordClass org.ramadda.data.point.icebridge.Igbgm2GravityV11Record -makefile 1 -filesuper org.ramadda.data.point.text.TextFile -super org.ramadda.data.point.PointRecord  -lineoriented 1 -delimiter { +}  -fields  {
    { year int }
    { dayOfYear int   }
    { secondOfDay int }
    { longitude double -unit degrees }
    { latitude double -unit degrees}
    { aircraftHeight double -unit m -chartable true}
    { freeAirGravityDisturbance double -unit mGal -chartable true}
}


##
##IceBridge Sander AIRGrav L1B Geolocated Free Air Gravity Anomalies
##ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE_FTP/IGGRV1B_AIRGravXyAnom_v01/
##

generateRecordClass org.ramadda.data.point.icebridge.Iggrv1bAirGravityRecord -makefile 1 -filesuper org.ramadda.data.point.text.TextFile -super org.ramadda.data.point.PointRecord  -lineoriented 1 -delimiter { +}  -skiplines 7 -fields  {
    { latitude double -unit degrees}
    { longitude double -unit degrees }
    { date int }
    { day int   }
    { flight int }
    { time double}
    { fiducial double} 
    { upsx double} 
    { upsy double} 
    { wgshgt double} 
    { fx double} 
    { fy double} 
    { fz double} 
    { eotgrav double} 
    { facor double} 
    { intcor double} 
    { fag070 double} 
    { fag100 double} 
    { fag140 double} 
    { fltenviro double} 
}



##Paris
##ftp://n4ftl01u.ecs.nasa.gov/SAN2/ICEBRIDGE/IRPAR2.001/
##

generateRecordClass org.ramadda.data.point.icebridge.ParisRecord -makefile 1 -filesuper org.ramadda.data.point.text.TextFile -super org.ramadda.data.point.PointRecord  -lineoriented 1 -delimiter { +} -fields  { 
    { lat double}
    { lon double}
    { time double}
    { thickness double -chartable true}
    { aircraftAltitude double}
    { confidence int -chartable true -searchable true}
}  -extraBody {
    public double getLatitude() {
        return lat;
    }
    public double getLongitude() {
        return lon;
    }
    public double getAltitude() {
        return aircraftAltitude;
    }

}

