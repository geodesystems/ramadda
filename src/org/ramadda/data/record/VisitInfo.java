/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.data.record;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class VisitInfo {

    /** _more_ */
    public static final boolean QUICKSCAN_YES = true;

    /** _more_ */
    public static final boolean QUICKSCAN_NO = false;


    /** _more_ */
    private int visitCount = 0;

    /** _more_ */
    private Hashtable<Object, Object> properties = new Hashtable<Object,
                                                       Object>();

    /** _more_ */
    private int skip = -1;

    /** _more_ */
    private int start = -1;

    /** _more_ */
    private int stop = -1;

    /** _more_ */
    private int max = -1;

    /** _more_ */
    private RecordIO recordIO;

    /** _more_ */
    private boolean quickScan = false;

    /** _more_ */
    private int recordIndex = 0;


    /**
     * _more_
     */
    public VisitInfo() {}

    /**
     * _more_
     *
     * @param recordIO _more_
     */
    public VisitInfo(RecordIO recordIO) {
        this.recordIO = recordIO;
    }


    /**
     * _more_
     *
     * @param that _more_
     */
    public VisitInfo(VisitInfo that) {
        this.quickScan  = that.quickScan;
        this.properties = new Hashtable<Object, Object>(that.properties);
        this.skip       = that.skip;
        this.start      = that.start;
        this.stop       = that.stop;
    }

    /**
     * _more_
     *
     * @param quickScan _more_
     */
    public VisitInfo(boolean quickScan) {
        this.quickScan = quickScan;
    }


    /**
     * _more_
     *
     * @param quickScan _more_
     * @param skip _more_
     */
    public VisitInfo(boolean quickScan, int skip) {
        this.quickScan = quickScan;
        this.skip      = skip;
    }


    /**
     * _more_
     *
     * @param skip _more_
     */
    public VisitInfo(int skip) {
        this.skip = skip;
    }

    /**
     * _more_
     *
     * @param skip _more_
     * @param start _more_
     * @param stop _more_
     */
    public VisitInfo(int skip, int start, int stop) {
        this.skip  = skip;
        this.start = start;
        this.stop  = stop;
    }

    /**
     *  Set the RecordIndex property.
     *
     *  @param value The new value for RecordIndex
     */
    public void setRecordIndex(int value) {
        recordIndex = value;
    }

    /**
     *  Get the RecordIndex property.
     *
     *  @return The RecordIndex
     */
    public int getRecordIndex() {
        return recordIndex;
    }

    /**
     * _more_
     *
     * @param delta _more_
     */
    public void addRecordIndex(int delta) {
        recordIndex += delta;
    }

    /**
     * Set the RecordIO property.
     *
     * @param value The new value for RecordIO
     */
    public void setRecordIO(RecordIO value) {
        recordIO = value;
    }

    /**
     * Get the RecordIO property.
     *
     * @return The RecordIO
     */
    public RecordIO getRecordIO() {
        return recordIO;
    }


    /**
     *  Set the QuickScan property.
     *
     *  @param value The new value for QuickScan
     */
    public void setQuickScan(boolean value) {
        quickScan = value;
    }

    /**
     *  Get the QuickScan property.
     *
     *  @return The QuickScan
     */
    public boolean getQuickScan() {
        return quickScan;
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(Object key, boolean dflt) {
        Boolean value = (Boolean) getProperty(key);
        if (value == null) {
            return dflt;
        }

        return value.booleanValue();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getCount() {
        return visitCount;
    }

    /**
     * _more_
     */
    public void incrCount() {
        visitCount++;
    }

    /**
     *  Set the Skip property.
     *
     *  @param value The new value for Skip
     */
    public void setSkip(int value) {
        skip = value;
    }

    /**
     *  Get the Skip property.
     *
     *  @return The Skip
     */
    public int getSkip() {
        return skip;
    }

    /**
     *  Set the Start property.
     *
     *  @param value The new value for Start
     */
    public void setStart(int value) {
        start = value;
    }


    /**
     *  Get the Start property.
     *
     *  @return The Start
     */
    public int getStart() {
        return start;
    }

    /**
     * Set the Max property.
     *
     * @param value The new value for Max
     */
    public void setMax(int value) {
        max = value;
    }

    /**
     * Get the Max property.
     *
     * @return The Max
     */
    public int getMax() {
        return max;
    }



    /**
     *  Set the Stop property.
     *
     *  @param value The new value for Stop
     */
    public void setStop(int value) {
        stop = value;
    }

    /**
     *  Get the Stop property.
     *
     *  @return The Stop
     */
    public int getStop() {
        return stop;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "visit info: skip =" + skip + " max=" + max + " stop: " + stop;
    }


}
