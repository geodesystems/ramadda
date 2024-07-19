/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import org.ramadda.util.Utils;

import java.text.SimpleDateFormat;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


/**
 * Base class for reading and writing binary and text records
 *
 *
 * @author   Jeff McWhirter
 */
public class BaseRecord implements Cloneable {



    /** _more_ */
    public static final boolean CHARTABLE_YES = true;

    /** _more_ */
    public static final boolean CHARTABLE_NO = false;

    /** _more_ */
    public static final boolean SEARCHABLE_YES = true;

    /** _more_ */
    public static final boolean SEARCHABLE_NO = false;


    /** _more_ */
    public static final String PROP_INCLUDEVECTOR = "includevector";

    /** _more_ */
    public static final long UNDEFINED_TIME = -9999;


    public enum ReadStatus { OK, SKIP, EOF }


    /** _more_ */
    public static final int ATTR_FIRST = 0;

    /** _more_ */
    public static final int ATTR_LAST = 0;



    /** The file */
    private RecordFile recordFile;

    /** The fields */
    private List<RecordField> fields;


    /** _more_ */
    public int index = -1;

    /** work array */
    private byte work[] = new byte[8];

    /** Is the binary data big endian */
    private boolean bigEndian = true;

    /** Are we in quick scan mode */
    private boolean quickScan = false;

    protected boolean skipProcessing = false;


    /** _more_ */
    private long recordTime = UNDEFINED_TIME;

    private SimpleDateFormat outputDateFormat;

    /**
     * _more_
     */
    public BaseRecord() {
    }



    /**
     * Ctor
     *
     * @param recordFile The file
     */
    public BaseRecord(RecordFile recordFile) {
        this.recordFile = recordFile;
    }

    /**
     * Copy ctor
     *
     * @param that what to copy
     */
    public BaseRecord(BaseRecord that) {
        this.recordFile = that.recordFile;

    }


    /**
     * Ctor
     *
     *
     * @param recordFile The file
     * @param bigEndian Is the binary data we read and write big endian
     */
    public BaseRecord(RecordFile recordFile, boolean bigEndian) {
        this.recordFile = recordFile;
        this.bigEndian  = bigEndian;
    }



    public void setOutputDateFormat(SimpleDateFormat sdf) {
	outputDateFormat=sdf;
    }


    public String formatDate(Date d) {
	if(outputDateFormat!=null) return outputDateFormat.format(d);
	return Utils.formatIso(d);
    }




    public int skipCnt = 0;
    public void setSkipProcessing(boolean v) {
	skipProcessing  = v;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getLastAttribute() {
        return ATTR_LAST;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean[] doMakeAttributeMask() {
        boolean[] mask = new boolean[getLastAttribute()];
        for (int i = 0; i < mask.length; i++) {
            mask[i] = true;
        }

        return mask;
    }

    /**
     * _more_
     *
     * @param mask _more_
     * @param fields _more_
     */
    public void initAttributeMask(boolean[] mask, HashSet<String> fields) {}


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
     */
    public void makeRandom() {}

    /**
     * _more_
     *
     * @param that _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean runRandomTest(BaseRecord that) throws Exception {
        this.makeRandom();
        this.write(new RecordIO(new FileOutputStream(getClass()
                + "test.dat")));
        that.read(new RecordIO(new FileInputStream(getClass() + "test.dat")));

        return this.equals(that);
    }

    /**
     *  Set the RecordFile property.
     *
     *  @param value The new value for RecordFile
     */
    public void setRecordFile(RecordFile value) {
        recordFile = value;
    }

    /**
     *  Get the RecordFile property.
     *
     *  @return The RecordFile
     */
    public RecordFile getRecordFile() {
        return recordFile;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<RecordField> getFields() {
        if (fields == null) {
            fields = new ArrayList<RecordField>();
            addFields(fields);
            initFields(fields);
        }

        return fields;
    }

    /**
     * _more_
     *
     * @param fields _more_
     */
    protected void addFields(List<RecordField> fields) {}

    /**
     * _more_
     *
     * @param fields _more_
     */
    public void initFields(List<RecordField> fields) {}

    /**
     * _more_
     *
     * @param s _more_
     * @param size _more_
     *
     * @return _more_
     */
    public static byte[] createByteArray(String s, int size) {
        byte[] b       = new byte[size];
        byte[] strings = s.getBytes();
        for (int i = 0; (i < b.length) && (i < strings.length); i++) {
            b[i] = strings[i];
        }

        return b;
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param values _more_
     *
     * @return _more_
     */
    public boolean haveValueInSet(String what, HashSet<String> values) {
        return false;
    }





    public int getIndex(String field) {
	for(RecordField f: getFields()) {
	    if(f.getName().equals(field)) return f.getParamId();
	}
	return -1;
    }


    public Date getDate() {
	for(RecordField f: getFields()) {
	    Object o = getObjectValue(f.getParamId());
	    if(o!=null && o instanceof Date) return (Date) o;
	}
	return null;
    }


    public double getValue(String field) {
	int index = getIndex(field);
	if(index<0) throw new IllegalArgumentException("Unknown field: " + field);
	return getValue(index);
    }

    public Object getObjectValue(String field) {
	int index = getIndex(field);
	if(index<0) throw new IllegalArgumentException("Unknown field: " + field);
	return getObjectValue(index);
    }	    


    /**
     * _more_
     *
     * @param attrId _more_
     *
     * @return _more_
     */
    public double getValue(int attrId) {
        throw new IllegalArgumentException("Unknown attribute id:" + attrId);
    }


    /**  */
    static int xcnt = 0;

    /**
     * _more_
     *
     * @param attrId _more_
     *
     * @return _more_
     */
    public String getStringValue(int attrId) {
        Object object = getObjectValue(attrId);
        if (object == null) {
            throw new IllegalArgumentException("Unknown attribute index:"
                    + attrId);
        }

        if (object instanceof Date) {
            Date dttm = (Date) object;
            return Utils.formatIso(dttm);
        }

        return object.toString();
    }

    /**
     * _more_
     *
     * @param attrId _more_
     *
     * @return _more_
     */
    public Object getObjectValue(int attrId) {
        throw new IllegalArgumentException("Unknown attribute id:" + attrId);
    }

    /**
     * _more_
     *
     * @param attrId _more_
     *
     * @return _more_
     */
    public boolean hasObjectValue(int attrId) {
        return false;
    }

    /**
     * _more_
     *
     * @param buff _more_
     *
     * @throws Exception _more_
     */
    public void print(Appendable buff) throws Exception {}


    /**
     * _more_
     *
     * @param recordIO _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public ReadStatus read(RecordIO recordIO) throws Exception {
        return ReadStatus.OK;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    public final ReadStatus readNextRecord(RecordIO recordIO)
            throws Exception {
        ReadStatus status = read(recordIO);
        if (status != ReadStatus.OK) {
            return status;
        }
        initializeAfterRead(recordIO);

        return status;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws IOException _more_
     */
    public void initializeAfterRead(RecordIO recordIO) throws IOException {}

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws IOException On badness
     */
    public void write(RecordIO recordIO) throws IOException {}

    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param pw _more_
     */
    public final void printCsv(VisitInfo visitInfo, PrintWriter pw) {
        doPrintCsv(visitInfo, pw);
        pw.print("\n");
    }

    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param pw _more_
     *
     * @return _more_
     */
    public int doPrintCsv(VisitInfo visitInfo, PrintWriter pw) {
        //        pw.append("#" + index+",");
        return 0;
    }

    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param pw _more_
     */
    public final void printCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
        pw.print("#");
        doPrintCsvHeader(visitInfo, pw);
        pw.print("\n");
    }

    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param pw _more_
     *
     * @return _more_
     */
    public int doPrintCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
        return 0;
    }



    /**
     * _more_
     *
     * @param value _more_
     * @param from _more_
     * @param to _more_
     * @param bit _more_
     *
     * @return _more_
     */
    public byte mask(byte value, int from, int to, boolean bit) {
        for (int i = from; i <= to; i++) {
            value &= (0xF & (bit
                             ? 1
                             : 0) << i);
        }

        return value;
    }


    /**
     *     _more_
     *
     *     @return _more_
     */
    public int getRecordSize() {
        return 0;
    }


    /**
     * _more_
     *
     * @param s _more_
     * @param bit _more_
     *
     * @return _more_
     */
    public boolean isBitSet(short s, int bit) {
        short mask = (short) (1 << bit);

        return (s & mask) != 0;
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param bit _more_
     * @param v _more_
     *
     * @return _more_
     */
    public short setBit(short s, int bit, boolean v) {
        short mask = (short) (1 << bit);
        if (v) {
            s = (short) (s | mask);
        } else {
            s = (short) (s & ~mask);
        }

        return s;
    }



    /**
     * _more_
     *
     * @param s _more_
     * @param bit _more_
     *
     * @return _more_
     */
    public boolean isBitSet(int s, int bit) {
        int mask = (int) (1 << bit);

        return (s & mask) != 0;
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param bit _more_
     * @param v _more_
     *
     * @return _more_
     */
    public int setBit(int s, int bit, boolean v) {
        int mask = (int) (1 << bit);
        if (v) {
            s = (int) (s | mask);
        } else {
            s = (int) (s & ~mask);
        }

        return s;
    }


    /**
     * _more_
     *
     * @param s _more_
     * @param bit _more_
     *
     * @return _more_
     */
    public boolean isBitSet(byte s, int bit) {
        byte mask = (byte) (1 << bit);

        return (s & mask) != 0;
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param bit _more_
     * @param v _more_
     *
     * @return _more_
     */
    public byte setBit(byte s, int bit, boolean v) {
        byte mask = (byte) (1 << bit);
        if (v) {
            s = (byte) (s | mask);
        } else {
            s = (byte) (s & ~mask);
        }

        return s;
    }




    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readInts(DataInputStream dis, int[] v) throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readInt(dis);
        }
    }



    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readFloats(DataInputStream dis, float[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readFloat(dis);
        }
    }

    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readBytes(DataInputStream dis, byte[] v) throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readByte(dis);
        }
    }


    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readShorts(DataInputStream dis, short[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readShort(dis);
        }
    }

    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readDoubles(DataInputStream dis, double[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readDouble(dis);
        }
    }


    /**
     * _more_
     *
     * @param dis _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public String readString(DataInputStream dis) throws IOException {
        return dis.readUTF();
    }


    /**
     * _more_
     *
     * @param dis _more_
     * @param v _more_
     *
     * @throws IOException _more_
     */
    public void readStrings(DataInputStream dis, String[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readString(dis);
        }
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException _more_
     */
    public void write(DataOutputStream dos, String[] v) throws IOException {
        for (int i = 0; i < v.length; i++) {
            dos.writeUTF(v[i]);
        }
    }

    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(String[] original, String[] newValue) {
        if (original.length < newValue.length) {
            throw new IllegalArgumentException("length mismatch "
                    + original.length + "<" + newValue.length);
        }
        System.arraycopy(newValue, 0, original, 0, newValue.length);
    }


    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readLongs(DataInputStream dis, long[] v) throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readLong(dis);
        }
    }


    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readUnsignedBytes(DataInputStream dis, short[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readUnsignedByte(dis);
        }
    }

    /**
     * _more_
     *
     * @param dis data input stream
     *
     * @return _more_
     *
     * @throws IOException On badness
     */
    public short readUnsignedByte(DataInputStream dis) throws IOException {
        readBytes(dis, work, 1);

        return unsignedByteToShort(work[0]);
    }


    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readUnsignedShorts(DataInputStream dis, int[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readUnsignedShort(dis);
        }
    }



    /**
     * _more_
     *
     * @param dis data input stream
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void readUnsignedInts(DataInputStream dis, long[] v)
            throws IOException {
        for (int i = 0; i < v.length; i++) {
            v[i] = readUnsignedInt(dis);
        }
    }


    /**
     * _more_
     *
     * @param b _more_
     *
     * @return _more_
     */
    public static final short unsignedByteToShort(byte b) {
        short i = 0;
        i |= b & 0xFF;

        return i;
    }

    /**
     * Read little endian long
     *
     * @param dis data input stream
     *
     * @return _more_
     *
     * @throws IOException On badness
     */
    public long readLong(DataInputStream dis) throws IOException {
        if ( !bigEndian) {
            return readLELong(dis);
        }

        return dis.readLong();
    }

    /**
     * Read little endian long
     *
     * @param dis data input stream
     *
     * @return _more_
     *
     * @throws IOException On badness
     */
    public long readLELong(DataInputStream dis) throws IOException {
        long accum = 0;
        for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
            // must cast to long or the shift would be done modulo 32
            accum |= (long) (dis.readByte() & 0xff) << shiftBy;
        }

        return accum;
    }




    /**
     * _more_
     *
     * @param dis data input stream
     *
     * @return _more_
     *
     * @throws IOException On badness
     */
    public short readShort(DataInputStream dis) throws IOException {
        int b1 = dis.readByte() & 0xff;
        int b2 = dis.readByte() & 0xff;
        if ( !bigEndian) {
            int tmp = b1;
            b1 = b2;
            b2 = tmp;
        }

        return (short) (b1 << 8 | b2);
    }


    /**
     * _more_
     *
     * @param dis data input stream
     *
     * @return _more_
     *
     * @throws IOException On badness
     */
    public int readUnsignedShort(DataInputStream dis) throws IOException {
        readBytes(dis, work, 2);

        return unsignedShortToInt(work);
    }


    /**
     * _more_
     *
     * @param dis data input stream
     *
     * @return _more_
     *
     * @throws IOException On badness
     */
    public long readUnsignedInt(DataInputStream dis) throws IOException {
        readBytes(dis, work, 4);

        return unsignedIntToLong(work);
    }


    /**
     * _more_
     *
     * @param b _more_
     *
     * @return _more_
     */
    public static final long unsignedIntToLong(byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;

        return l;
    }


    /**
     * _more_
     *
     * @param dis data input stream
     * @param array _more_
     * @param cnt _more_
     *
     * @throws IOException On badness
     */
    private void readBytes(DataInputStream dis, byte[] array, int cnt)
            throws IOException {
        for (int i = 0; i < cnt; i++) {
            array[i] = dis.readByte();
        }
        if ( !bigEndian) {
            flip(array, cnt);
        }
    }


    /**
     * _more_
     *
     * @param array _more_
     * @param cnt _more_
     */
    private void flip(byte[] array, int cnt) {
        byte tmp;
        if (cnt == 2) {
            tmp      = array[0];
            array[0] = array[1];
            array[1] = tmp;
        } else if (cnt == 4) {
            tmp      = array[0];
            array[0] = array[3];
            array[3] = tmp;
            tmp      = array[1];
            array[1] = array[2];
            array[2] = tmp;
        } else if (cnt == 8) {
            tmp      = array[0];
            array[0] = array[3];
            array[3] = tmp;
            tmp      = array[1];
            array[1] = array[2];
            array[2] = tmp;
        } else if (cnt == 1) {
            //noop
        } else {
            throw new IllegalArgumentException("bad flip count:" + cnt);
        }
    }


    /**
     * Converts a two byte array to an integer
     * @param b a byte array of length 2
     * @return an int representing the unsigned short
     */
    public static final int unsignedShortToInt(byte[] b) {
        int i = 0;
        i |= b[0] & 0xFF;
        i <<= 8;
        i |= b[1] & 0xFF;

        return i;
    }


    /**
     * Read an int
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public int readInt(DataInputStream dis) throws IOException {
        if ( !bigEndian) {
            return readLEInt(dis);
        }

        return dis.readInt();
    }

    /**
     * Read a little endian integer
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public int readLEInt(DataInputStream dis) throws IOException {
        int accum = 0;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= (dis.readByte() & 0xff) << shiftBy;
        }

        return accum;
    }






    /**
     * _more_
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public double readDouble(DataInputStream dis) throws IOException {
        if ( !bigEndian) {
            return readLEDouble(dis);
        }

        return dis.readDouble();
    }

    /**
     * Read little endian double
     *
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public double readLEDouble(DataInputStream dis) throws IOException {
        long accum = 0;
        for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
            // must cast to long or the shift would be done modulo 32
            accum |= ((long) (dis.readByte() & 0xff)) << shiftBy;
        }

        return Double.longBitsToDouble(accum);
    }


    /**
     * read a byte
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public byte readByte(DataInputStream dis) throws IOException {
        return dis.readByte();
    }

    /**
     * _more_
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public float readFloat(DataInputStream dis) throws IOException {
        if ( !bigEndian) {
            return readLEFloat(dis);
        }

        return dis.readFloat();
    }


    /**
     * Read little endian float
     *
     * @param dis data input stream
     *
     * @return the value
     *
     * @throws IOException On badness
     */
    public float readLEFloat(DataInputStream dis) throws IOException {
        int accum = 0;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= (dis.readByte() & 0xff) << shiftBy;
        }

        return Float.intBitsToFloat(accum);
    }



    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeByte(DataOutputStream dos, byte v) throws IOException {
        dos.write(v);
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param b _more_
     */
    public void toBytes(int value, byte[] b) {
        for (int i = 0; i < 4; i++) {
            int offset = (4 - 1 - i) * 8;
            b[i] = (byte) ((value >> offset) & 0xFF);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param b _more_
     */
    public void toBytes(short value, byte[] b) {
        for (int i = 0; i < 2; i++) {
            int offset = (2 - 1 - i) * 8;
            b[i] = (byte) ((value >> offset) & 0xFF);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param b _more_
     */
    public void toBytes(long value, byte[] b) {
        for (int i = 0; i < 8; i++) {
            int offset = (8 - 1 - i) * 8;
            b[i] = (byte) ((value >> offset) & 0xFF);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param b _more_
     */
    public void toBytes(double value, byte[] b) {
        toBytes(Double.doubleToLongBits(value), b);
    }

    /**
     * _more_
     *
     * @param value _more_
     * @param b _more_
     */
    public void toBytes(float value, byte[] b) {
        toBytes(Float.floatToIntBits(value), b);
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeInt(DataOutputStream dos, int v) throws IOException {
        toBytes(v, work);
        writeEndian(dos, work, 4);
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param a _more_
     * @param cnt _more_
     *
     * @throws IOException On badness
     */
    private void writeEndian(DataOutputStream dos, byte[] a, int cnt)
            throws IOException {
        if (bigEndian) {
            dos.write(a, 0, cnt);
        } else {
            for (int i = cnt - 1; i >= 0; i--) {
                dos.writeByte(a[i]);
            }
        }
    }




    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeLong(DataOutputStream dos, long v) throws IOException {
        toBytes(v, work);
        writeEndian(dos, work, 8);
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeDouble(DataOutputStream dos, double v)
            throws IOException {
        toBytes(v, work);
        writeEndian(dos, work, 8);
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeFloat(DataOutputStream dos, float v) throws IOException {
        toBytes(v, work);
        writeEndian(dos, work, 4);
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeShort(DataOutputStream dos, short v) throws IOException {
        toBytes(v, work);
        writeEndian(dos, work, 2);
    }




    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeUnsignedByte(DataOutputStream dos, short v)
            throws IOException {
        toBytes(v, work);
        writeByte(dos, work[1]);
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeUnsignedBytes(DataOutputStream dos, short[] v)
            throws IOException {
        for (short l : v) {
            writeUnsignedByte(dos, l);
        }
    }




    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeUnsignedShort(DataOutputStream dos, int v)
            throws IOException {
        toBytes((short) v, work);
        writeEndian(dos, work, 2);
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeUnsignedShorts(DataOutputStream dos, int[] v)
            throws IOException {
        for (int l : v) {
            writeUnsignedShort(dos, l);
        }
    }






    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeUnsignedInt(DataOutputStream dos, long v)
            throws IOException {
        toBytes((int) v, work);
        writeEndian(dos, work, 4);
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void writeUnsignedInts(DataOutputStream dos, long[] v)
            throws IOException {
        for (long l : v) {
            writeUnsignedInt(dos, l);
        }
    }




    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void write(DataOutputStream dos, byte[] v) throws IOException {
        dos.write(v, 0, v.length);
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void write(DataOutputStream dos, long[] v) throws IOException {
        for (long l : v) {
            writeLong(dos, l);
        }
    }

    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void write(DataOutputStream dos, int[] v) throws IOException {
        for (int l : v) {
            writeInt(dos, l);
        }
    }



    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void write(DataOutputStream dos, short[] v) throws IOException {
        for (short l : v) {
            writeShort(dos, l);
        }
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void write(DataOutputStream dos, float[] v) throws IOException {
        for (float l : v) {
            writeFloat(dos, l);
        }
    }


    /**
     * _more_
     *
     * @param dos _more_
     * @param v _more_
     *
     * @throws IOException On badness
     */
    public void write(DataOutputStream dos, double[] v) throws IOException {
        for (double l : v) {
            writeDouble(dos, l);
        }
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(byte[] original, byte[] newValue) {
        if (original.length < newValue.length) {
            throw new IllegalArgumentException("length mismatch "
                    + original.length + "<" + newValue.length);
        }
        for (int i = 0; i < original.length; i++) {
            original[i] = 0;
        }
        System.arraycopy(newValue, 0, original, 0, newValue.length);
    }

    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(short[] original, short[] newValue) {
        if (original.length < newValue.length) {
            throw new IllegalArgumentException("length mismatch "
                    + original.length + "<" + newValue.length);
        }
        for (int i = 0; i < original.length; i++) {
            original[i] = 0;
        }
        System.arraycopy(newValue, 0, original, 0, newValue.length);
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(int[] original, int[] newValue) {
        if (original.length < newValue.length) {
            throw new IllegalArgumentException("length mismatch "
                    + original.length + "<" + newValue.length);
        }
        for (int i = 0; i < original.length; i++) {
            original[i] = 0;
        }
        System.arraycopy(newValue, 0, original, 0, newValue.length);
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(double[] to, double[] from) {
        if (to.length < from.length) {
            throw new IllegalArgumentException("length mismatch "
                    + to.length + "<" + from.length);
        }
        for (int i = 0; i < to.length; i++) {
            to[i] = 0;
        }
	//	Utils.print("BaseRecord.copy: from:",from); 
        System.arraycopy(from, 0, to, 0, from.length);
	//	Utils.print("BaseRecord.copy: to:",to); 
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(float[] original, float[] newValue) {
        if (original.length < newValue.length) {
            throw new IllegalArgumentException("length mismatch "
                    + original.length + "<" + newValue.length);
        }
        for (int i = 0; i < original.length; i++) {
            original[i] = 0f;
        }
        System.arraycopy(newValue, 0, original, 0, newValue.length);
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param newValue _more_
     */
    public void copy(long[] original, long[] newValue) {
        if (original.length < newValue.length) {
            throw new IllegalArgumentException("length mismatch "
                    + original.length + "<" + newValue.length);
        }
        for (int i = 0; i < original.length; i++) {
            original[i] = 0;
        }
        System.arraycopy(newValue, 0, original, 0, newValue.length);
    }

    /**
     * _more_
     *
     * @param a _more_
     *
     * @return _more_
     */
    public float[] toFloat(short[] a) {
        if (a == null) {
            return null;
        }
        float[] f = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            f[i] = (float) a[i];
        }

        return f;
    }



    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public boolean equals(Object object) {
        if ( !(object instanceof BaseRecord)) {
            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public long getRecordTime() {
        return recordTime;
    }

    /**
     * _more_
     *
     * @param time _more_
     */
    public void setRecordTime(long time) {
        this.recordTime = time;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasRecordTime() {
        return getRecordTime() != UNDEFINED_TIME;
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param v _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(RecordField field, double v) {
        return getRecordFile().isMissingValue(this, field, v);
    }


    /**
     * _more_
     *
     * @param field _more_
     * @param s _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(RecordField field, String s) {
        return getRecordFile().isMissingValue(this, field, s);
    }


    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getStringValue(RecordField field, long value) {
        return "" + value;
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getStringValue(RecordField field, double value) {
        return "" + value;
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getStringValue(RecordField field, float value) {
        return "" + value;
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getStringValue(RecordField field, int value) {
        return "" + value;
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getStringValue(RecordField field, short value) {
        return "" + value;
    }


    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getStringValue(RecordField field, byte value) {
        return "" + value;
    }

    public static void main(String[] args) throws Exception {
	DataOutputStream dos = new DataOutputStream(new FileOutputStream("test.out"));
	BaseRecord record = new BaseRecord();
	double[]out = {77,24};
	double[]in = {0,0};	
	for(int i=0;i<5;i++) {
	    record.write(dos,out);
	}
	dos.close();
	DataInputStream dis = new DataInputStream(new FileInputStream("test.out"));
	for(int i=0;i<5;i++) {
	    record.readDoubles(dis,in);
	    for(double d:in)
		System.err.print("," + d);
	    System.err.println("");
	}
    }


}
