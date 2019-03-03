
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
public class LasExtendedVariableLengthRecord extends Record {
    public static final int ATTR_FIRST = Record.ATTR_LAST;
    public static final List<RecordField> FIELDS = new ArrayList<RecordField>();
    public static final int ATTR_RESERVED =  ATTR_FIRST + 1;
    public static final RecordField RECORDATTR_RESERVED;
    public static final int ATTR_USERID =  ATTR_FIRST + 2;
    public static final RecordField RECORDATTR_USERID;
    public static final int ATTR_RECORDID =  ATTR_FIRST + 3;
    public static final RecordField RECORDATTR_RECORDID;
    public static final int ATTR_RECORDLENGTHAFTERHEADER =  ATTR_FIRST + 4;
    public static final RecordField RECORDATTR_RECORDLENGTHAFTERHEADER;
    public static final int ATTR_DESCRIPTION =  ATTR_FIRST + 5;
    public static final RecordField RECORDATTR_DESCRIPTION;
    public static final int ATTR_LAST = ATTR_FIRST + 6;
    

    static {
    FIELDS.add(RECORDATTR_RESERVED = new RecordField("Reserved", "Reserved", "", ATTR_RESERVED, "", "short", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RESERVED.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasExtendedVariableLengthRecord)record).Reserved;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasExtendedVariableLengthRecord)record).Reserved;
    }
    });
    FIELDS.add(RECORDATTR_USERID = new RecordField("userId", "userId", "", ATTR_USERID, "", "string[16]", "byte", 16, SEARCHABLE_NO,CHARTABLE_NO));
    FIELDS.add(RECORDATTR_RECORDID = new RecordField("recordId", "recordId", "", ATTR_RECORDID, "", "short", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RECORDID.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasExtendedVariableLengthRecord)record).recordId;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasExtendedVariableLengthRecord)record).recordId;
    }
    });
    FIELDS.add(RECORDATTR_RECORDLENGTHAFTERHEADER = new RecordField("recordLengthAfterHeader", "recordLengthAfterHeader", "", ATTR_RECORDLENGTHAFTERHEADER, "", "short", "short", 0, SEARCHABLE_NO,CHARTABLE_NO));
    RECORDATTR_RECORDLENGTHAFTERHEADER.setValueGetter(new ValueGetter() {
    public double getValue(Record record, RecordField field, VisitInfo visitInfo) {
    return (double) ((LasExtendedVariableLengthRecord)record).recordLengthAfterHeader;
    }
    public String getStringValue(Record record, RecordField field, VisitInfo visitInfo) {
    return ""+ ((LasExtendedVariableLengthRecord)record).recordLengthAfterHeader;
    }
    });
    FIELDS.add(RECORDATTR_DESCRIPTION = new RecordField("Description", "Description", "", ATTR_DESCRIPTION, "", "string[32]", "byte", 32, SEARCHABLE_NO,CHARTABLE_NO));
    
    }
    

    short Reserved;
    byte[] userId = new byte[16];
    String userIdAsString;
    short recordId;
    short recordLengthAfterHeader;
    byte[] Description = new byte[32];
    String DescriptionAsString;
    

    public  LasExtendedVariableLengthRecord(LasExtendedVariableLengthRecord that)  {
        super(that);
        this.Reserved = that.Reserved;
        this.userId = that.userId;
        this.recordId = that.recordId;
        this.recordLengthAfterHeader = that.recordLengthAfterHeader;
        this.Description = that.Description;
        
        
    }



    public  LasExtendedVariableLengthRecord(RecordFile file)  {
        super(file);
    }



    public  LasExtendedVariableLengthRecord(RecordFile file, boolean bigEndian)  {
        super(file, bigEndian);
    }



    public int getLastAttribute()  {
        return ATTR_LAST;
    }



    public  boolean equals(Object object)  {
        if(!super.equals(object)) {System.err.println("bad super"); return false;} if(!(object instanceof LasExtendedVariableLengthRecord)) return false;
        LasExtendedVariableLengthRecord that = (LasExtendedVariableLengthRecord ) object;
        if(this.Reserved!= that.Reserved) {System.err.println("bad Reserved");  return false;}
        if(!java.util.Arrays.equals(this.userId, that.userId)) {System.err.println("bad userId"); return false;}
        if(this.recordId!= that.recordId) {System.err.println("bad recordId");  return false;}
        if(this.recordLengthAfterHeader!= that.recordLengthAfterHeader) {System.err.println("bad recordLengthAfterHeader");  return false;}
        if(!java.util.Arrays.equals(this.Description, that.Description)) {System.err.println("bad Description"); return false;}
        return true;
    }




    protected void addFields(List<RecordField> fields)  {
        super.addFields(fields);
        fields.addAll(FIELDS);
    }



    public double getValue(int attrId)  {
        if(attrId == ATTR_RESERVED) return Reserved;
        if(attrId == ATTR_RECORDID) return recordId;
        if(attrId == ATTR_RECORDLENGTHAFTERHEADER) return recordLengthAfterHeader;
        return super.getValue(attrId);
        
    }



    public int getRecordSize()  {
        return super.getRecordSize() + 54;
    }



    public ReadStatus read(RecordIO recordIO) throws Exception  {
        DataInputStream dis = recordIO.getDataInputStream();
        Reserved =  readShort(dis);
        readBytes(dis,userId);
        recordId =  readShort(dis);
        recordLengthAfterHeader =  readShort(dis);
        readBytes(dis,Description);
        
        
        return ReadStatus.OK;
    }



    public void write(RecordIO recordIO) throws IOException  {
        DataOutputStream dos = recordIO.getDataOutputStream();
        writeShort(dos, Reserved);
        write(dos, userId);
        writeShort(dos, recordId);
        writeShort(dos, recordLengthAfterHeader);
        write(dos, Description);
        
    }



    public int doPrintCsv(VisitInfo visitInfo,PrintWriter pw)  {
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        int superCnt = super.doPrintCsv(visitInfo,pw);
        int myCnt = 0;
        if(superCnt>0) pw.print(',');
        pw.print(getStringValue(RECORDATTR_RESERVED, Reserved));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RECORDID, recordId));
        myCnt++;
        pw.print(',');
        pw.print(getStringValue(RECORDATTR_RECORDLENGTHAFTERHEADER, recordLengthAfterHeader));
        myCnt++;
        if(includeVector) {
        for(int i=0;i<this.userId.length;i++) {pw.print(i==0?'|':',');pw.print(this.userId[i]);}
        myCnt++;
        }
        if(includeVector) {
        for(int i=0;i<this.Description.length;i++) {pw.print(i==0?'|':',');pw.print(this.Description[i]);}
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public int doPrintCsvHeader(VisitInfo visitInfo,PrintWriter pw)  {
        int superCnt = super.doPrintCsvHeader(visitInfo,pw);
        int myCnt = 0;
        boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR, false);
        if(superCnt>0) pw.print(',');
        RECORDATTR_RESERVED.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RECORDID.printCsvHeader(visitInfo,pw);
        myCnt++;
        pw.print(',');
        RECORDATTR_RECORDLENGTHAFTERHEADER.printCsvHeader(visitInfo,pw);
        myCnt++;
        if(includeVector) {
        pw.print(',');
        RECORDATTR_USERID.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        if(includeVector) {
        pw.print(',');
        RECORDATTR_DESCRIPTION.printCsvHeader(visitInfo,pw);
        myCnt++;
        }
        return myCnt+superCnt;
        
    }



    public void print(Appendable buff)  throws Exception  {
        super.print(buff);
        buff.append(" Reserved: " + Reserved+" \n");
        buff.append(" userId: " + getUserIdAsString()+" \n");
        buff.append(" recordId: " + recordId+" \n");
        buff.append(" recordLengthAfterHeader: " + recordLengthAfterHeader+" \n");
        buff.append(" Description: " + getDescriptionAsString()+" \n");
        
    }



    public short getReserved()  {
        return Reserved;
    }


    public void setReserved(short newValue)  {
        Reserved = newValue;
    }


    public byte[] getUserId()  {
        return userId;
    }


    public String getUserIdAsString()  {
        if(userIdAsString==null) userIdAsString =new String(userId);
        return userIdAsString;
    }


    public void setUserId(byte[] newValue)  {
        copy(userId, newValue);
    }


    public void setUserId(String  newValue)  {
        copy(userId, newValue.getBytes());
    }


    public short getRecordId()  {
        return recordId;
    }


    public void setRecordId(short newValue)  {
        recordId = newValue;
    }


    public short getRecordLengthAfterHeader()  {
        return recordLengthAfterHeader;
    }


    public void setRecordLengthAfterHeader(short newValue)  {
        recordLengthAfterHeader = newValue;
    }


    public byte[] getDescription()  {
        return Description;
    }


    public String getDescriptionAsString()  {
        if(DescriptionAsString==null) DescriptionAsString =new String(Description);
        return DescriptionAsString;
    }


    public void setDescription(byte[] newValue)  {
        copy(Description, newValue);
    }


    public void setDescription(String  newValue)  {
        copy(Description, newValue.getBytes());
    }



}



