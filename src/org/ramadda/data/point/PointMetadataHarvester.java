/**
   Copyright (c) 2008-2021 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point;


import org.ramadda.data.record.*;
import org.ramadda.util.grid.LatLonGrid;

import java.io.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;

import java.util.List;
import java.util.Properties;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class PointMetadataHarvester extends RecordVisitor {

    /** _more_ */
    private int cnt = 0;

    /** _more_ */
    private int badCnt = 0;

    private boolean force = false;
    
    /** _more_ */
    private double minElevation = Double.NaN;

    /** _more_ */
    private double maxElevation = Double.NaN;

    /** _more_ */
    private double minLatitude = Double.NaN;

    /** _more_ */
    private double maxLatitude = Double.NaN;

    /** _more_ */
    private double minLongitude = Double.NaN;

    /** _more_ */
    private double maxLongitude = Double.NaN;

    /** _more_ */
    private long minTime = Long.MAX_VALUE;

    /** _more_ */
    private long maxTime = Long.MIN_VALUE;

    /** _more_ */
    private LatLonGrid llg;

    /** _more_ */
    private Properties properties;

    /** _more_ */
    private double[][] ranges;

    /** _more_ */
    List<RecordField> fields;

    Hashtable<String,HashSet<String>> enumSamples = new Hashtable<String,HashSet<String>>();

    /**
     * _more_
     */
    public PointMetadataHarvester() {}


    /**
     * _more_
     *
     * @param llg _more_
     */
    public PointMetadataHarvester(LatLonGrid llg) {
        this.llg = llg;
    }


    /**
       Set the Force property.

       @param value The new value for Force
    **/
    public void setForce (boolean value) {
	force = value;
    }

    /**
       Get the Force property.

       @return The Force
    **/
    public boolean getForce () {
	return force;
    }


    public HashSet<String> getSamples(String field) {
	return enumSamples.get(field);
    }

    /**
     *  @return _more_
     */
    public List<RecordField> getFields() {
        return fields;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasTimeRange() {
        return minTime != Long.MAX_VALUE;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public long getMinTime() {
        return minTime;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public long getMaxTime() {
        return maxTime;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     */
    @Override
    public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                               BaseRecord record) {


        PointRecord pointRecord = (PointRecord) record;
        double      lat         = pointRecord.getLatitude();
        double      lon         = pointRecord.getLongitude();

        if (ranges == null) {
            fields = pointRecord.getFields();
            ranges = new double[fields.size()][2];
            for (double[] range : ranges) {
                range[0] = Double.NaN;
                range[1] = Double.NaN;
            }
        }

        boolean skipRecord = ( !pointRecord.isValidPosition()
                               && pointRecord.needsValidPosition());

        //        System.err.println("skip:" + skipRecord + " lat: " + lat + " " + lon);
        //Skip this if it doesn't have a valid position
        if (skipRecord) {
            if (Double.isNaN(lat) && Double.isNaN(lon)) {
                //TODO:what do do with undefined
            } else {
                badCnt++;
                if (badCnt < 10) {
                    System.err.println(
				       "PointMetadataHarvester: bad position: " + lat + " "
				       + lon);
                }
                if ((badCnt > 1000) && ((cnt == 0) || (badCnt > 10 * cnt))) {
                    System.err.println(
				       "PointMetadataHarvester:Too many bad locations. Something must be wrong.");

                    return false;
                }
            }

            return true;
        }


        for (int fieldCnt = 0; fieldCnt < fields.size(); fieldCnt++) {
            RecordField field = fields.get(fieldCnt);
	    if(field.isTypeEnumeration() || field.isTypeString()) {
		ValueGetter valueGetter = field.getValueGetter();
                if (valueGetter == null) {
                    continue;
                }
		HashSet<String> samples = enumSamples.get(field.getName());
		if(samples==null)  {
		    samples = new HashSet<String>();
		    enumSamples.put(field.getName(),samples);
		}
		if(samples.size()<50) {
		    String  value = valueGetter.getStringValue(pointRecord, field,
							       visitInfo);
		    if(value!=null) {
			if(!samples.contains(value)) {
			    //			    System.err.println("Field:" +field +" VALUE:" + value);
			    samples.add(value);
			}
		    }
		}

	    }

            if (field.isTypeNumeric()) {
		ValueGetter valueGetter = field.getValueGetter();
                if (valueGetter == null) {
                    continue;
                }
                double value = valueGetter.getValue(pointRecord, field,
						    visitInfo);
                if (pointRecord.isMissingValue(field, value)) {
                    continue;
                }
                if ( !Double.isNaN(value)) {
                    if (Double.isNaN(ranges[fieldCnt][0])) {
                        ranges[fieldCnt][0] = value;
                    } else {
                        ranges[fieldCnt][0] = Math.min(value,
						       ranges[fieldCnt][0]);
                    }
                    if (Double.isNaN(ranges[fieldCnt][1])) {
                        ranges[fieldCnt][1] = value;
                    } else {
                        ranges[fieldCnt][1] = Math.max(value,
						       ranges[fieldCnt][1]);
                    }
                }
            }
	}
        cnt++;
        if (llg != null) {
            llg.incrementCount(lat, lon);
        }

        long time = record.getRecordTime();
        if (BaseRecord.UNDEFINED_TIME != time) {
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
        }


        minLatitude  = getMin(minLatitude, pointRecord.getLatitude());
        maxLatitude  = getMax(maxLatitude, pointRecord.getLatitude());
        minLongitude = getMin(minLongitude, pointRecord.getLongitude());
        maxLongitude = getMax(maxLongitude, pointRecord.getLongitude());
        minElevation = getMin(minElevation, pointRecord.getAltitude());
        maxElevation = getMax(maxElevation, pointRecord.getAltitude());

        return true;
    }



    /**
     * _more_
     *
     * @param value1 _more_
     * @param value2 _more_
     *
     * @return _more_
     */
    private double getMin(double value1, double value2) {
        if (Double.isNaN(value1)) {
            return value2;
        }
        if (Double.isNaN(value2)) {
            return value1;
        }

        return Math.min(value1, value2);
    }

    /**
     * _more_
     *
     * @param value1 _more_
     * @param value2 _more_
     *
     * @return _more_
     */
    private double getMax(double value1, double value2) {
        if (Double.isNaN(value1)) {
            return value2;
        }
        if (Double.isNaN(value2)) {
            return value1;
        }

        return Math.max(value1, value2);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getCount() {
        return cnt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("latitude:" + minLatitude + " - " + maxLatitude);
        sb.append("\n");
        sb.append("longitude:" + minLongitude + " - " + maxLongitude);
        sb.append("\n");
        sb.append("elevation:" + minElevation + " - " + maxElevation);
        sb.append("\n");


        if (hasTimeRange()) {
            sb.append("time:" + " " + new Date(getMinTime()) + " -- "
                      + new Date(getMaxTime()));
            sb.append("\n");
        }


        if (fields != null) {
            for (int fieldCnt = 0; fieldCnt < fields.size(); fieldCnt++) {
                RecordField field = fields.get(fieldCnt);
                if (field.isTypeNumeric()) {
                    ValueGetter valueGetter = field.getValueGetter();
                    if (valueGetter == null) {
                        continue;
                    }
                    //Skip the arrays
                    if (field.getArity() > 1) {
                        continue;
                    }
                    sb.append(field.getName() + ": " + ranges[fieldCnt][0]
                              + " " + ranges[fieldCnt][1] + "\n");
                }
            }
        }


        return sb.toString();

    }




    /**
     * Get the MinLatitude property.
     *
     * @return The MinLatitude
     */
    public double getMinLatitude() {
        return this.minLatitude;
    }



    /**
     * Get the MaxLatitude property.
     *
     * @return The MaxLatitude
     */
    public double getMaxLatitude() {
        return this.maxLatitude;
    }


    /**
     * Get the MinLongitude property.
     *
     * @return The MinLongitude
     */
    public double getMinLongitude() {
        return this.minLongitude;
    }


    /**
     * Get the MaxLongitude property.
     *
     * @return The MaxLongitude
     */
    public double getMaxLongitude() {
        return this.maxLongitude;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getMinElevation() {
        return this.minElevation;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getMaxElevation() {
        return this.maxElevation;
    }


    /**
     *  Get the Properties property.
     *
     *  @return The Properties
     */
    public Properties getProperties() {
        return properties;
    }



}
