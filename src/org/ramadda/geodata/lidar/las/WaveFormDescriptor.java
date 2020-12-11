
/*
 * Copyright 2013 ramadda.org
 * http://ramadda.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package org.ramadda.geodata.lidar.las;

import org.ramadda.data.record.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import org.ramadda.geodata.lidar.*;





/** This is generated code from generate.tcl. Do not edit it! */
public class WaveFormDescriptor extends BaseRecord {
    public static final int ATTR_FIRST = BaseRecord.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_BITSPERSAMPLE =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_BITSPERSAMPLE;
    public static final int ATTR_WAVEFORMCOMPRESSIONTYPE =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_WAVEFORMCOMPRESSIONTYPE;
    public static final int ATTR_NUMBEROFSAMPLES =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_NUMBEROFSAMPLES;
    public static final int ATTR_TEMPORALSAMPLESPACING =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_TEMPORALSAMPLESPACING;
    public static final int ATTR_DIGITIZERGAIN =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_DIGITIZERGAIN;
    public static final int ATTR_DIGITIZEROFFSET =  ATTR_FIRST + 6;
    public static final RecordField RECORDATTR_DIGITIZEROFFSET;
    public static final int ATTR_LAST = ATTR_FIRST + 7;
    

    static {
    FIELDS.add(RECORDATTR_BITSPERSAMPLE = new RecordField("bitsPerSample", "bitsPerSample", "", ATTR_BITSPERSAMPLE, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_BITSPERSAMPLE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((WaveFormDescriptor)record).bitsPerSample;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((WaveFormDescriptor)record).bitsPerSample;
    }
    });
    FIELDS.add(RECORDATTR_WAVEFORMCOMPRESSIONTYPE = new RecordField("waveformCompressionType", "waveformCompressionType", "", ATTR_WAVEFORMCOMPRESSIONTYPE, "", "byte", "byte", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_WAVEFORMCOMPRESSIONTYPE.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((WaveFormDescriptor)record).waveformCompressionType;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((WaveFormDescriptor)record).waveformCompressionType;
    }
    });
    FIELDS.add(RECORDATTR_NUMBEROFSAMPLES = new RecordField("numberOfSamples", "numberOfSamples", "", ATTR_NUMBEROFSAMPLES, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_NUMBEROFSAMPLES.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((WaveFormDescriptor)record).numberOfSamples;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((WaveFormDescriptor)record).numberOfSamples;
    }
    });
    FIELDS.add(RECORDATTR_TEMPORALSAMPLESPACING = new RecordField("temporalSampleSpacing", "temporalSampleSpacing", "", ATTR_TEMPORALSAMPLESPACING, "", "uint", "int", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_TEMPORALSAMPLESPACING.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((WaveFormDescriptor)record).temporalSampleSpacing;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((WaveFormDescriptor)record).temporalSampleSpacing;
    }
    });
    FIELDS.add(RECORDATTR_DIGITIZERGAIN = new RecordField("digitizerGain", "digitizerGain", "", ATTR_DIGITIZERGAIN, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_DIGITIZERGAIN.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((WaveFormDescriptor)record).digitizerGain;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((WaveFormDescriptor)record).digitizerGain;
    }
    });
    FIELDS.add(RECORDATTR_DIGITIZEROFFSET = new RecordField("digitizerOffset", "digitizerOffset", "", ATTR_DIGITIZEROFFSET, "", "double", "double", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_DIGITIZEROFFSET.setValueGetter(new ValueGetter() {
    public double getValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return (double) ((WaveFormDescriptor)record).digitizerOffset;
    }
    public String getStringValue(BaseRecord record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((WaveFormDescriptor)record).digitizerOffset;
    }
    });
    
    }
    

    byte bitsPerSample;
    byte waveformCompressionType;
    long numberOfSamples;
    long temporalSampleSpacing;
    double digitizerGain;
    double digitizerOffset;
    

    public  WaveFormDescriptor(WaveFormDescriptor that)  {
        super(that);
        this.bitsPerSample = that.bitsPerSample;
        this.waveformCompressionType = that.waveformCompressionType;
        this.numberOfSamples = that.numberOfSamples;
        this.temporalSampleSpacing = that.temporalSampleSpacing;
        this.digitizerGain = that.digitizerGain;
        this.digitizerOffset = that.digitizerOffset;
        
        
    }



    public  WaveFormDescriptor(RecordFile file)  {
        super(file);
    }



    public  WaveFormDescriptor(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof WaveFormDescriptor)) return false;
        WaveFormDescriptor that = (WaveFormDescriptor ) object;
        if(this.bitsPerSample!= that.bitsPerSample) {System.err.println("bad bitsPerSample");  return false;}
        if(this.waveformCompressionType!= that.waveformCompressionType) {System.err.println("bad waveformCompressionType");  return false;}
        if(this.numberOfSamples!= that.numberOfSamples) {System.err.println("bad numberOfSamples");  return false;}
        if(this.temporalSampleSpacing!= that.temporalSampleSpacing) {System.err.println("bad temporalSampleSpacing");  return false;}
        if(this.digitizerGain!= that.digitizerGain) {System.err.println("bad digitizerGain");  return false;}
        if(this.digitizerOffset!= that.digitizerOffset) {System.err.println("bad digitizerOffset");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_BITSPERSAMPLE) return bitsPerSample;
        if(attrId == ATTR_WAVEFORMCOMPRESSIONTYPE) return waveformCompressionType;
        if(attrId == ATTR_NUMBEROFSAMPLES) return numberOfSamples;
        if(attrId == ATTR_TEMPORALSAMPLESPACING) return temporalSampleSpacing;
        if(attrId == ATTR_DIGITIZERGAIN) return digitizerGain;
        if(attrId == ATTR_DIGITIZEROFFSET) return digitizerOffset;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 26;
    }



    public BaseRecord.ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        bitsPerSample =  readByte(dis);
        waveformCompressionType =  readByte(dis);
        numberOfSamples =  readUnsignedInt(dis);
        temporalSampleSpacing =  readUnsignedInt(dis);
        digitizerGain =  readDouble(dis);
        digitizerOffset =  readDouble(dis);
        
        
        return BaseRecord.ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        writeByte(dos, bitsPerSample);
        writeByte(dos, waveformCompressionType);
        writeUnsignedInt(dos, numberOfSamples);
        writeUnsignedInt(dos, temporalSampleSpacing);
        writeDouble(dos, digitizerGain);
        writeDouble(dos, digitizerOffset);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_BITSPERSAMPLE, bitsPerSample));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_WAVEFORMCOMPRESSIONTYPE, waveformCompressionType));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_NUMBEROFSAMPLES, numberOfSamples));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_TEMPORALSAMPLESPACING, temporalSampleSpacing));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_DIGITIZERGAIN, digitizerGain));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_DIGITIZEROFFSET, digitizerOffset));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_BITSPERSAMPLE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_WAVEFORMCOMPRESSIONTYPE.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_NUMBEROFSAMPLES.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_TEMPORALSAMPLESPACING.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_DIGITIZERGAIN.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_DIGITIZEROFFSET.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" bitsPerSample: " + bitsPerSample+" \n");
        buff.append(" waveformCompressionType: " + waveformCompressionType+" \n");
        buff.append(" numberOfSamples: " + numberOfSamples+" \n");
        buff.append(" temporalSampleSpacing: " + temporalSampleSpacing+" \n");
        buff.append(" digitizerGain: " + digitizerGain+" \n");
        buff.append(" digitizerOffset: " + digitizerOffset+" \n");
        
    }



    public byte getBitsPerSample()  {
        return bitsPerSample;
    }


    public void setBitsPerSample(byte newValue)  {
        bitsPerSample = newValue;
    }


    public byte getWaveformCompressionType()  {
        return waveformCompressionType;
    }


    public void setWaveformCompressionType(byte newValue)  {
        waveformCompressionType = newValue;
    }


    public long getNumberOfSamples()  {
        return numberOfSamples;
    }


    public void setNumberOfSamples(long newValue)  {
        numberOfSamples = newValue;
    }


    public long getTemporalSampleSpacing()  {
        return temporalSampleSpacing;
    }


    public void setTemporalSampleSpacing(long newValue)  {
        temporalSampleSpacing = newValue;
    }


    public double getDigitizerGain()  {
        return digitizerGain;
    }


    public void setDigitizerGain(double newValue)  {
        digitizerGain = newValue;
    }


    public double getDigitizerOffset()  {
        return digitizerOffset;
    }


    public void setDigitizerOffset(double newValue)  {
        digitizerOffset = newValue;
    }



}



