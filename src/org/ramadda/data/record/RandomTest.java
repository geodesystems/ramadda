/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RandomTest extends BaseRecord {
    byte b = (byte) (Math.random() * 1000);
    short s = (short) (Math.random() * 1000);
    int i = (int) (Math.random() * 1000);
    long l = (long) (Math.random() * 1000);
    double d = (double) (Math.random() * 1000);
    float f = (float) (Math.random() * 1000);
    short u_b = (short) (Math.random() * 255);
    int u_s = (int) (Math.random() * 1000);
    long u_i = (long) (Math.random() * 1000);
    byte[] a_b = new byte[] { (byte) (Math.random() * 1000),
                              (byte) (Math.random() * 1000) };

    short[] a_s = new short[] { (short) (Math.random() * 1000),
                                (short) (Math.random() * 1000) };

    int[] a_i = new int[] { (int) (Math.random() * 1000),
                            (int) (Math.random() * 1000) };

    float[] a_f = new float[] { (float) (Math.random() * 1000),
                                (float) (Math.random() * 1000) };

    long[] a_l = new long[] { (long) (Math.random() * 1000),
                              (long) (Math.random() * 1000) };

    double[] a_d = new double[] { (double) (Math.random() * 1000),
                                  (double) (Math.random() * 1000) };

    short[] u_a_b = new short[] { (short) (Math.random() * 255),
                                  (short) (Math.random() * 255) };

    int[] u_a_s = new int[] { (int) (Math.random() * 1000),
                              (int) (Math.random() * 1000) };

    long[] u_a_i = new long[] { (long) (Math.random() * 1000),
                                (long) (Math.random() * 1000) };

    public RandomTest(boolean bigEndian) {
        super(null, bigEndian);
    }

    public void check(RandomTest that) {
        /*
        if (this.b != that.b) {
            throw new IllegalStateException("b error " + this.b + " "
                                            + that.b);
        }
        if (this.s != that.s) {
            throw new IllegalStateException("s error " + this.s + " "
                                            + that.s);
        }
        if (this.i != that.i) {
            throw new IllegalStateException("i error " + this.i + " "
                                            + that.i);
        }
        if (this.l != that.l) {
            throw new IllegalStateException("l error " + this.l + " "
                                            + that.l);
        }
        if (this.d != that.d) {
            throw new IllegalStateException("d error " + this.d + " "
                                            + that.d);
        }
        if (this.f != that.f) {
            throw new IllegalStateException("f error " + this.f + " "
                                            + that.f);
        }
        if (this.u_b != that.u_b) {
            throw new IllegalStateException("u_b error " + this.u_b + " "
                                            + that.u_b);
        }
*/
        if (this.u_s != that.u_s) {
            throw new IllegalStateException("u_s error " + this.u_s + " "
                                            + that.u_s);
        }
        /*
        if (this.u_i != that.u_i) {
            throw new IllegalStateException("u_i error " + this.u_i + " "
                                            + that.u_i);
        }
        if ( !Arrays.equals(this.a_b, that.a_b)) {
            throw new IllegalStateException("a_b error ");
        }
        if ( !Arrays.equals(this.a_s, that.a_s)) {
            throw new IllegalStateException("a_s error ");
        }
        if ( !Arrays.equals(this.a_i, that.a_i)) {
            throw new IllegalStateException("a_i error ");
        }
        if ( !Arrays.equals(this.a_f, that.a_f)) {
            throw new IllegalStateException("a_f error ");
        }
        if ( !Arrays.equals(this.a_d, that.a_d)) {
            throw new IllegalStateException("a_d error ");
        }
        if ( !Arrays.equals(this.a_l, that.a_l)) {
            throw new IllegalStateException("a_l error ");
        }
        if ( !Arrays.equals(this.u_a_b, that.u_a_b)) {
            throw new IllegalStateException("u_a_b error ");
        }
        if ( !Arrays.equals(this.u_a_s, that.u_a_s)) {
            throw new IllegalStateException("u_a_s error ");
        }
        if ( !Arrays.equals(this.u_a_i, that.u_a_i)) {
            throw new IllegalStateException("u_a_i error ");
        }
        */
    }

    public ReadStatus read(RecordIO recordIO) throws Exception {
        super.read(recordIO);
        this.b   = readByte(recordIO.getDataInputStream());
        this.s   = readShort(recordIO.getDataInputStream());
        this.i   = readInt(recordIO.getDataInputStream());
        this.l   = readLong(recordIO.getDataInputStream());
        this.d   = readDouble(recordIO.getDataInputStream());
        this.f   = readFloat(recordIO.getDataInputStream());

        this.u_b = readUnsignedByte(recordIO.getDataInputStream());
        this.u_s = readUnsignedShort(recordIO.getDataInputStream());
        this.u_i = readUnsignedInt(recordIO.getDataInputStream());

        readBytes(recordIO.getDataInputStream(), a_b);
        readShorts(recordIO.getDataInputStream(), a_s);
        readInts(recordIO.getDataInputStream(), a_i);
        readFloats(recordIO.getDataInputStream(), a_f);
        readDoubles(recordIO.getDataInputStream(), a_d);
        readLongs(recordIO.getDataInputStream(), a_l);

        readUnsignedBytes(recordIO.getDataInputStream(), u_a_b);
        readUnsignedShorts(recordIO.getDataInputStream(), u_a_s);
        readUnsignedInts(recordIO.getDataInputStream(), u_a_i);

        return ReadStatus.OK;
    }

    public void write(RecordIO recordIO) throws IOException {
        super.write(recordIO);

        writeByte(recordIO.getDataOutputStream(), b);
        writeShort(recordIO.getDataOutputStream(), s);
        writeInt(recordIO.getDataOutputStream(), i);
        writeLong(recordIO.getDataOutputStream(), l);
        writeDouble(recordIO.getDataOutputStream(), d);
        writeFloat(recordIO.getDataOutputStream(), f);
        writeUnsignedByte(recordIO.getDataOutputStream(), u_b);

        writeUnsignedShort(recordIO.getDataOutputStream(), u_s);

        writeUnsignedInt(recordIO.getDataOutputStream(), u_i);
        write(recordIO.getDataOutputStream(), a_b);
        write(recordIO.getDataOutputStream(), a_s);
        write(recordIO.getDataOutputStream(), a_i);
        write(recordIO.getDataOutputStream(), a_f);
        write(recordIO.getDataOutputStream(), a_d);
        write(recordIO.getDataOutputStream(), a_l);
        writeUnsignedBytes(recordIO.getDataOutputStream(), u_a_b);
        writeUnsignedShorts(recordIO.getDataOutputStream(), u_a_s);
        writeUnsignedInts(recordIO.getDataOutputStream(), u_a_i);
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 2; i++) {
            System.err.println("test " + i);
            String           filename = "test" + i + ".dat";
            FileOutputStream fos      = new FileOutputStream(filename);
            RandomTest       out      = new RandomTest(i == 0);
            RandomTest       in       = new RandomTest(i == 0);
            out.write(new RecordIO(fos));
            fos.close();

            FileInputStream fis = new FileInputStream(filename);
            DataInputStream dis = new DataInputStream(fis);
            in.read(new RecordIO(fis));
            out.check(in);
            in.check(out);
            System.err.println("   ok");
        }

    }

}
