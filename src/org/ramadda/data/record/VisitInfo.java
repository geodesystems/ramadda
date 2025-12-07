/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import java.io.*;

import org.ramadda.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


public class VisitInfo {
    public static final boolean QUICKSCAN_YES = true;
    public static final boolean QUICKSCAN_NO = false;
    private int visitCount = 0;
    private Hashtable<Object, Object> properties = new Hashtable<Object,
                                                       Object>();

    private int skip = -1;
    private int start = -1;
    private int stop = -1;
    private int max = -1;
    private int last = -1;
    private Date startDate;
    private Date endDate;    
    private RecordIO recordIO;
    private boolean quickScan = false;
    private int recordIndex = 0;

    public VisitInfo() {}

    public VisitInfo(RecordIO recordIO) {
        this.recordIO = recordIO;
    }

    public VisitInfo(VisitInfo that) {
        this.quickScan  = that.quickScan;
        this.properties = new Hashtable<Object, Object>(that.properties);
        this.skip       = that.skip;
        this.start      = that.start;
        this.stop       = that.stop;
    }

    public VisitInfo(boolean quickScan) {
        this.quickScan = quickScan;
    }

    public VisitInfo(boolean quickScan, int skip) {
        this.quickScan = quickScan;
        this.skip      = skip;
    }

    public VisitInfo(int skip) {
        this.skip = skip;
    }

    public VisitInfo(int skip, int start, int stop) {
        this.skip  = skip;
        this.start = start;
        this.stop  = stop;
    }

    public void setRecordIndex(int value) {
        recordIndex = value;
    }

    public int getRecordIndex() {
        return recordIndex;
    }

    public void addRecordIndex(int delta) {
        recordIndex += delta;
    }

    public void setRecordIO(RecordIO value) {
        recordIO = value;
    }

    public RecordIO getRecordIO() {
        return recordIO;
    }

    public void setQuickScan(boolean value) {
        quickScan = value;
    }


    public boolean getQuickScan() {
        return quickScan;
    }

    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public boolean getProperty(Object key, boolean dflt) {
        Boolean value = (Boolean) getProperty(key);
        if (value == null) {
            return dflt;
        }

        return value.booleanValue();
    }

    public int getCount() {
        return visitCount;
    }

    public void incrCount() {
        visitCount++;
    }

    public void setSkip(int value) {
        skip = value;
    }

    public int getSkip() {
        return skip;
    }

    public void setStart(int value) {
        start = value;
    }

    public int getStart() {
        return start;
    }

    public void setMax(int value) {
        max = value;
	//	System.err.println("VisitInfo.setMax:" +max+Utils.getStack(10));
    }


    public int getMax() {
        return max;
    }

    public void setLast (int value) {
	last = value;
    }

    public int getLast () {
	return last;
    }

    public void setStartDate (Date value) {
	startDate = value;
    }

    public Date getStartDate () {
	return startDate;
    }

    public void setEndDate (Date value) {
	endDate = value;
    }

    public Date getEndDate () {
	return endDate;
    }

    public void setStop(int value) {
	stop = value;
    }

    public int getStop() {
        return stop;
    }

    public String toString() {
        return "visit info: skip =" + skip + " max=" + max + " stop: " + stop +" last:" + last;
    }

}
