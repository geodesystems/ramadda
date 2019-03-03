
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
public class LasHeader_V13 extends LasHeader_V12 {
    public static final int ATTR_FIRST = LasHeader_V12.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_STARTOFWAVEFORMDATAPACKETRECORD =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_STARTOFWAVEFORMDATAPACKETRECORD;
    public static final int ATTR_LAST = ATTR_FIRST + 2;
    

    static {
    FIELDS.add(RECORDATTR_STARTOFWAVEFORMDATAPACKETRECORD = new RecordField("startOfWaveformDataPacketRecord", "startOfWaveformDataPacketRecord", "", ATTR_STARTOFWAVEFORMDATAPACKETRECORD, "", "long", "long", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_STARTOFWAVEFORMDATAPACKETRECORD.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasHeader_V13)record).startOfWaveformDataPacketRecord;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasHeader_V13)record).startOfWaveformDataPacketRecord;
    }
    });
    
    }
    

    long startOfWaveformDataPacketRecord;
    

    public  LasHeader_V13(LasHeader_V13 that)  {
        super(that);
        this.startOfWaveformDataPacketRecord = that.startOfWaveformDataPacketRecord;
        
        
    }



    public  LasHeader_V13(RecordFile file)  {
        super(file);
    }



    public  LasHeader_V13(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LasHeader_V13)) return false;
        LasHeader_V13 that = (LasHeader_V13 ) object;
        if(this.startOfWaveformDataPacketRecord!= that.startOfWaveformDataPacketRecord) {System.err.println("bad startOfWaveformDataPacketRecord");  return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_STARTOFWAVEFORMDATAPACKETRECORD) return startOfWaveformDataPacketRecord;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 8;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        ReadStatus status= super.read(recordIO);
        if(status!=ReadStatus.OK)  return status;
        startOfWaveformDataPacketRecord =  readLong(dis);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        super.write(recordIO);
        writeLong(dos, startOfWaveformDataPacketRecord);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_STARTOFWAVEFORMDATAPACKETRECORD, startOfWaveformDataPacketRecord));
        myCnt++;
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_STARTOFWAVEFORMDATAPACKETRECORD.printCsvHeader(visitInfo,pw);
        myCnt++;
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" startOfWaveformDataPacketRecord: " + startOfWaveformDataPacketRecord+" \n");
        
    }



    public long getStartOfWaveformDataPacketRecord()  {
        return startOfWaveformDataPacketRecord;
    }


    public void setStartOfWaveformDataPacketRecord(long newValue)  {
        startOfWaveformDataPacketRecord = newValue;
    }



}



