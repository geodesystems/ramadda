
package nom.tam.fits.test;


import junit.framework.JUnit4TestAdapter;

import nom.tam.fits.*;

import nom.tam.util.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.*;

import java.lang.reflect.*;


/**
 * This class tests the binary table classes for
 *  the Java FITS library, notably BinaryTableHDU,
 *  BinaryTable, FitsHeap and the utility class ColumnTable.
 *  Tests include:
 *  <pre>
 *     Reading and writing data of all valid types.
 *     Reading and writing variable length da
 *     Creating binary tables from:
 *        Object[][] array
 *        Object[] array
 *        ColumnTable
 *        Column x Column
 *        Row x Row
 *     Read binary table
 *        Row x row
 *        Element x element
 *     Modify
 *        Row, column, element
 *     Rewrite binary table in place
 * </pre>
 */
public class BinaryTableTest {

    /** _more_ */
    byte[] bytes = new byte[50];

    /** _more_ */
    byte[][] bits = new byte[50][2];

    /** _more_ */
    boolean[] bools = new boolean[50];

    /** _more_ */
    short[][] shorts = new short[50][3];

    /** _more_ */
    int[] ints = new int[50];

    /** _more_ */
    float[][][] floats = new float[50][4][4];

    /** _more_ */
    double[] doubles = new double[50];

    /** _more_ */
    long[] longs = new long[50];

    /** _more_ */
    String[] strings = new String[50];

    /** _more_ */
    float[][] vf = new float[50][];

    /** _more_ */
    short[][] vs = new short[50][];

    /** _more_ */
    double[][] vd = new double[50][];

    /** _more_ */
    boolean[][] vbool = new boolean[50][];

    /** _more_ */
    float[][][] vc = new float[50][][];

    /** _more_ */
    double[][][] vdc = new double[50][][];

    /** _more_ */
    float[][] complex = new float[50][2];

    /** _more_ */
    float[][][] complex_arr = new float[50][4][2];

    /** _more_ */
    double[][] dcomplex = new double[50][2];

    /** _more_ */
    double[][][] dcomplex_arr = new double[50][4][2];

    /**
     * _more_
     */
    @Before
    public void initialize() {

        for (int i = 0; i < bytes.length; i += 1) {
            bytes[i]     = (byte) (2 * i);
            bits[i][0]   = bytes[i];
            bits[i][1]   = (byte) ( ~bytes[i]);
            bools[i]     = (bytes[i] % 8) == 0
                           ? true
                           : false;

            shorts[i][0] = (short) (2 * i);
            shorts[i][1] = (short) (3 * i);
            shorts[i][2] = (short) (4 * i);

            ints[i]      = i * i;
            for (int j = 0; j < 4; j += 1) {
                for (int k = 0; k < 4; k += 1) {
                    floats[i][j][k] = (float) (i + j * Math.exp(k));
                }
            }
            doubles[i]    = 3 * Math.sin(i);
            longs[i]      = i * i * i * i;
            strings[i]    = "abcdefghijklmnopqrstuvwxzy".substring(0, i % 20);

            vf[i]         = new float[i + 1];
            vf[i][i / 2]  = i * 3;
            vs[i]         = new short[i / 10 + 1];
            vs[i][i / 10] = (short) -i;
            vd[i]         = new double[(i % 2 == 0)
                                       ? 1
                                       : 2];
            vd[i][0]      = 99.99;
            vbool[i]      = new boolean[i / 10];
            if (i >= 10) {
                vbool[i][0] = i % 2 == 1;
            }

            int m5 = i % 5;
            vc[i] = new float[m5][];
            for (int j = 0; j < m5; j += 1) {
                vc[i][j]    = new float[2];
                vc[i][j][0] = i;
                vc[i][j][1] = -j;
            }
            vdc[i] = new double[m5][];
            for (int j = 0; j < m5; j += 1) {
                vdc[i][j]    = new double[2];
                vdc[i][j][0] = -j;
                vdc[i][j][1] = i;
            }
            double rad = 2 * i * Math.PI / bytes.length;
            complex[i][0]  = (float) (Math.cos(rad));
            complex[i][1]  = (float) (Math.sin(rad));
            dcomplex[i][0] = complex[i][0];
            dcomplex[i][1] = complex[i][1];
            for (int j = 0; j < 4; j += 1) {
                complex_arr[i][j][0]  = (j + 1) * complex[i][0];
                complex_arr[i][j][1]  = (j + 1) * complex[i][1];
                dcomplex_arr[i][j][0] = (j + 1) * complex[i][0];
                dcomplex_arr[i][j][1] = (j + 1) * complex[i][1];
            }
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testSimpleIO() throws Exception {

        FitsFactory.setUseAsciiTables(false);

        Fits     f    = new Fits();
        Object[] data = new Object[] {
            bytes, bits, bools, shorts, ints, floats, doubles, longs, strings,
            complex, dcomplex, complex_arr, dcomplex_arr
        };
        f.addHDU(Fits.makeHDU(data));

        BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
        bhdu.setColumnName(0, "bytes", null);
        bhdu.setColumnName(1, "bits", "bits later on");
        bhdu.setColumnName(6, "doubles", null);
        bhdu.setColumnName(5, "floats", "4 x 4 array");

        BufferedFile bf = new BufferedFile("bt1.fits", "rw");
        f.write(bf);
        bf.flush();
        bf.close();


        f = new Fits("bt1.fits");
        f.read();

        assertEquals("NHDU", 2, f.getNumberOfHDUs());


        BinaryTableHDU thdu = (BinaryTableHDU) f.getHDU(1);
        Header         hdr  = thdu.getHeader();

        assertEquals("HDR1", data.length, hdr.getIntValue("TFIELDS"));
        assertEquals("HDR2", 2, hdr.getIntValue("NAXIS"));
        assertEquals("HDR3", 8, hdr.getIntValue("BITPIX"));
        assertEquals("HDR4", "BINTABLE", hdr.getStringValue("XTENSION"));
        assertEquals("HDR5", "bytes", hdr.getStringValue("TTYPE1"));
        assertEquals("HDR6", "doubles", hdr.getStringValue("TTYPE7"));

        for (int i = 0; i < data.length; i += 1) {
            Object col = thdu.getColumn(i);
            if (i == 8) {
                String[] st = (String[]) col;

                for (int j = 0; j < st.length; j += 1) {
                    st[j] = st[j].trim();
                }
            }
            assertEquals("Data" + i, true,
                         ArrayFuncs.arrayEquals(data[i], col));
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testSimpleComplex() throws Exception {
        try {
            FitsFactory.setUseAsciiTables(false);

            Fits     f    = new Fits();
            Object[] data = new Object[] {
                bytes, bits, bools, shorts, ints, floats, doubles, longs,
                strings, complex, dcomplex, complex_arr, dcomplex_arr
            };
            BinaryTableHDU bhdu = (BinaryTableHDU) Fits.makeHDU(data);

            bhdu.setComplexColumn(9);
            bhdu.setComplexColumn(10);
            bhdu.setComplexColumn(11);
            bhdu.setComplexColumn(12);

            f.addHDU(bhdu);
            bhdu.setColumnName(9, "Complex1", null);

            BufferedFile bf = new BufferedFile("bt1c.fits", "rw");
            f.write(bf);
            bf.flush();
            bf.close();

            f = new Fits("bt1c.fits");
            f.read();

            assertEquals("NHDUc", 2, f.getNumberOfHDUs());


            BinaryTableHDU thdu = null;
            thdu = (BinaryTableHDU) f.getHDU(1);
            Header hdr = thdu.getHeader();

            for (int i = 0; i < data.length; i += 1) {

                Object col = thdu.getColumn(i);
                if (i == 8) {
                    String[] st = (String[]) col;

                    for (int j = 0; j < st.length; j += 1) {
                        st[j] = st[j].trim();
                    }
                }
                int n = Array.getLength(data[i]);

                assertEquals("DataC" + i, true,
                             ArrayFuncs.arrayEquals(data[i], col));
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);

            throw e;
        }

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testRowDelete() throws Exception {
        Fits f = new Fits("bt1.fits");
        f.read();

        BinaryTableHDU thdu = (BinaryTableHDU) f.getHDU(1);

        assertEquals("Del1", 50, thdu.getNRows());
        thdu.deleteRows(10, 20);
        assertEquals("Del2", 30, thdu.getNRows());

        double[] dbl = (double[]) thdu.getColumn(6);
        assertEquals("del3", dbl[9], doubles[9], 0);
        assertEquals("del4", dbl[10], doubles[30], 0);

        BufferedFile bf = new BufferedFile("bt1x.fits", "rw");
        f.write(bf);
        bf.close();

        f = new Fits("bt1x.fits");
        f.read();
        thdu = (BinaryTableHDU) f.getHDU(1);
        dbl  = (double[]) thdu.getColumn(6);
        assertEquals("del5", 30, thdu.getNRows());
        assertEquals("del6", 13, thdu.getNCols());
        assertEquals("del7", dbl[9], doubles[9], 0);
        assertEquals("del8", dbl[10], doubles[30], 0);

        thdu.deleteRows(20);
        assertEquals("del9", 20, thdu.getNRows());
        dbl = (double[]) thdu.getColumn(6);
        assertEquals("del10", 20, dbl.length);
        assertEquals("del11", dbl[0], doubles[0], 0);
        assertEquals("del12", dbl[19], doubles[39], 0);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testVar() throws Exception {
        try {
            Object[] data = new Object[] {
                floats, vf, vs, vd, shorts, vbool, vc, vdc
            };
            BasicHDU hdu  = Fits.makeHDU(data);
            Fits     f    = new Fits();
            f.addHDU(hdu);
            BufferedDataOutputStream bdos = new BufferedDataOutputStream(
                                                new FileOutputStream(
                                                    "bt2.fits"));
            f.write(bdos);
            bdos.close();

            f = new Fits("bt2.fits");
            f.read();
            BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
            Header         hdr  = bhdu.getHeader();

            assertEquals("var1", true, hdr.getIntValue("PCOUNT") > 0);
            assertEquals("var2", data.length, hdr.getIntValue("TFIELDS"));

            for (int i = 0; i < data.length; i += 1) {
                assertEquals("vardata" + i, true,
                             ArrayFuncs.arrayEquals(data[i],
                                 bhdu.getColumn(i)));
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);

            throw e;
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testSet() throws Exception {

        Fits f = new Fits("bt2.fits");
        f.read();
        BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
        Header         hdr  = bhdu.getHeader();

        // Check the various set methods on variable length data.
        float[] dta = (float[]) bhdu.getElement(4, 1);
        dta = new float[] { 22, 21, 20 };
        bhdu.setElement(4, 1, dta);

        BufferedDataOutputStream bdos =
            new BufferedDataOutputStream(new FileOutputStream("bt2a.fits"));
        f.write(bdos);
        bdos.close();

        f    = new Fits("bt2a.fits");
        bhdu = (BinaryTableHDU) f.getHDU(1);
        float[] xdta = (float[]) bhdu.getElement(4, 1);

        assertEquals("ts1", true, ArrayFuncs.arrayEquals(dta, xdta));
        assertEquals("ts2", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(3, 1), vf[3]));
        assertEquals("ts4", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(5, 1), vf[5]));

        assertEquals("ts5", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), dta));

        float tvf[] = new float[] { 101, 102, 103, 104 };
        vf[4] = tvf;

        bhdu.setColumn(1, vf);
        assertEquals("ts6", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(3, 1), vf[3]));
        assertEquals("ts7", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), vf[4]));
        assertEquals("ts8", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(5, 1), vf[5]));

        bdos = new BufferedDataOutputStream(
            new FileOutputStream("bt2b.fits"));
        f.write(bdos);
        bdos.close();

        f    = new Fits("bt2b.fits");
        bhdu = (BinaryTableHDU) f.getHDU(1);
        assertEquals("ts9", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(3, 1), vf[3]));
        assertEquals("ts10", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), vf[4]));
        assertEquals("ts11", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(5, 1), vf[5]));

        Object[] rw  = bhdu.getRow(4);

        float[]  trw = new float[] {
            -1, -2, -3, -4, -5, -6
        };
        rw[1] = trw;

        bhdu.setRow(4, rw);
        assertEquals("ts12", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(3, 1), vf[3]));
        assertEquals("ts13", false,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), vf[4]));
        assertEquals("ts14", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), trw));
        assertEquals("ts15", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(5, 1), vf[5]));

        bdos = new BufferedDataOutputStream(
            new FileOutputStream("bt2c.fits"));
        f.write(bdos);
        bdos.close();

        f    = new Fits("bt2c.fits");
        bhdu = (BinaryTableHDU) f.getHDU(1);
        assertEquals("ts16", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(3, 1), vf[3]));
        assertEquals("ts17", false,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), vf[4]));
        assertEquals("ts18", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(4, 1), trw));
        assertEquals("ts19", true,
                     ArrayFuncs.arrayEquals(bhdu.getElement(5, 1), vf[5]));
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void buildByColumn() throws Exception {

        BinaryTable btab = new BinaryTable();

        btab.addColumn(floats);
        btab.addColumn(vf);
        btab.addColumn(strings);
        btab.addColumn(vbool);
        btab.addColumn(ints);
        btab.addColumn(vc);
        btab.addColumn(complex);

        Fits f = new Fits();
        f.addHDU(Fits.makeHDU(btab));

        BufferedDataOutputStream bdos =
            new BufferedDataOutputStream(new FileOutputStream("bt3.fits"));
        f.write(bdos);

        f = new Fits("bt3.fits");
        BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
        btab = (BinaryTable) bhdu.getData();

        assertEquals("col1", true,
                     ArrayFuncs.arrayEquals(floats, bhdu.getColumn(0)));
        assertEquals("col2", true,
                     ArrayFuncs.arrayEquals(vf, bhdu.getColumn(1)));
        assertEquals("col6", true,
                     ArrayFuncs.arrayEquals(vc, bhdu.getColumn(5)));
        assertEquals("col7", true,
                     ArrayFuncs.arrayEquals(complex, bhdu.getColumn(6)));

        String[] col = (String[]) bhdu.getColumn(2);
        for (int i = 0; i < col.length; i += 1) {
            col[i] = col[i].trim();
        }
        assertEquals("coi3", true, ArrayFuncs.arrayEquals(strings, col));

        assertEquals("col4", true,
                     ArrayFuncs.arrayEquals(vbool, bhdu.getColumn(3)));
        assertEquals("col5", true,
                     ArrayFuncs.arrayEquals(ints, bhdu.getColumn(4)));
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void buildByRow() throws Exception {

        Fits f = new Fits("bt2.fits");
        f.read();
        BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
        Header         hdr  = bhdu.getHeader();
        BinaryTable    btab = (BinaryTable) bhdu.getData();
        for (int i = 0; i < 50; i += 1) {

            Object[]  row = btab.getRow(i);
            float[]   qx  = (float[]) row[1];
            float[][] p   = (float[][]) row[0];
            p[0][0] = (float) (i * Math.sin(i));
            btab.addRow(row);
        }

        f = new Fits();
        f.addHDU(Fits.makeHDU(btab));
        BufferedFile bf = new BufferedFile("bt4.fits", "rw");
        f.write(bf);
        bf.flush();
        bf.close();

        f    = new Fits("bt4.fits");

        btab = (BinaryTable) f.getHDU(1).getData();
        assertEquals("row1", 100, btab.getNRows());


        // Try getting data before we read in the table.

        float[][][] xf = (float[][][]) btab.getColumn(0);
        assertEquals("row2", (float) 0., xf[50][0][0], 0);
        assertEquals("row3", (float) (49 * Math.sin(49)), xf[99][0][0], 0);

        for (int i = 0; i < xf.length; i += 3) {

            boolean[] ba   = (boolean[]) btab.getElement(i, 5);
            float[]   fx   = (float[]) btab.getElement(i, 1);

            int       trow = i % 50;

            assertEquals("row4", true,
                         ArrayFuncs.arrayEquals(ba, vbool[trow]));
            assertEquals("row6", true, ArrayFuncs.arrayEquals(fx, vf[trow]));

        }
        float[][][] cmplx = (float[][][]) btab.getColumn(6);
        for (int i = 0; i < vc.length; i += 1) {
            for (int j = 0; j < vc[i].length; j += 1) {
                assertEquals("rowvc" + i + "_" + j, true,
                             ArrayFuncs.arrayEquals(vc[i][j],
                                 cmplx[i + vc.length][j]));
            }
        }
        // Fill the table.
        f.getHDU(1).getData();

        xf = (float[][][]) btab.getColumn(0);
        assertEquals("row7", 0.F, xf[50][0][0], 0);
        assertEquals("row8", (float) (49 * Math.sin(49)), xf[99][0][0], 0);

        for (int i = 0; i < xf.length; i += 3) {

            boolean[] ba   = (boolean[]) btab.getElement(i, 5);
            float[]   fx   = (float[]) btab.getElement(i, 1);

            int       trow = i % 50;

            assertEquals("row9", true,
                         ArrayFuncs.arrayEquals(ba, vbool[trow]));
            assertEquals("row11", true, ArrayFuncs.arrayEquals(fx, vf[trow]));

        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testObj() throws Exception {

        /** * Create a binary table from an Object[][] array */
        Object[][] x = new Object[5][3];
        for (int i = 0; i < 5; i += 1) {
            x[i][0] = new float[] { i };
            x[i][1] = new String("AString" + i);
            x[i][2] = new int[][] {
                { i, 2 * i }, { 3 * i, 4 * i }
            };
        }

        Fits     f   = new Fits();
        BasicHDU hdu = Fits.makeHDU(x);
        f.addHDU(hdu);
        BufferedFile bf = new BufferedFile("bt5.fits", "rw");
        f.write(bf);
        bf.close();

        /** Now get rid of some columns */
        BinaryTableHDU xhdu = (BinaryTableHDU) hdu;


        // First column
        assertEquals("delcol1", 3, xhdu.getNCols());
        xhdu.deleteColumnsIndexOne(1, 1);
        assertEquals("delcol2", 2, xhdu.getNCols());

        xhdu.deleteColumnsIndexZero(1, 1);
        assertEquals("delcol3", 1, xhdu.getNCols());

        bf = new BufferedFile("bt6.fits", "rw");
        f.write(bf);

        f    = new Fits("bt6.fits");

        xhdu = (BinaryTableHDU) f.getHDU(1);
        assertEquals("delcol4", 1, xhdu.getNCols());
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testDegenerate() throws Exception {

        String[] sa = new String[10];
        int[][]  ia = new int[10][0];
        Fits     f  = new Fits();

        for (int i = 0; i < sa.length; i += 1) {
            sa[i] = "";
        }

        Object[]       data = new Object[] { sa, ia };
        BinaryTableHDU bhdu = (BinaryTableHDU) Fits.makeHDU(data);
        Header         hdr  = bhdu.getHeader();
        f.addHDU(bhdu);
        BufferedFile bf = new BufferedFile("bt7.fits", "rw");
        f.write(bf);
        bf.close();

        assertEquals("degen1", 2, hdr.getIntValue("TFIELDS"));
        assertEquals("degen2", 10, hdr.getIntValue("NAXIS2"));
        assertEquals("degen3", 0, hdr.getIntValue("NAXIS1"));

        f    = new Fits("bt7.fits");
        bhdu = (BinaryTableHDU) f.getHDU(1);

        hdr  = bhdu.getHeader();
        assertEquals("degen4", 2, hdr.getIntValue("TFIELDS"));
        assertEquals("degen5", 10, hdr.getIntValue("NAXIS2"));
        assertEquals("degen6", 0, hdr.getIntValue("NAXIS1"));
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testDegen2() throws Exception {
        FitsFactory.setUseAsciiTables(false);

        Object[] data = new Object[] {
            new String[] {
                "a", "b", "c", "d", "e", "f"
            }, new int[] {
                1, 2, 3, 4, 5, 6
            }, new float[] {
                1.f, 2.f, 3.f, 4.f, 5.f, 6.f
            }, new String[] {
                "", "", "", "", "", ""
            }, new String[] {
                "a", "", "c", "", "e", "f"
            }, new String[] {
                "", "b", "c", "d", "e", "f"
            }, new String[] {
                "a", "b", "c", "d", "e", ""
            }, new String[] {
                null, null, null, null, null, null
            }, new String[] {
                "a", null, "c", null, "e", "f"
            }, new String[] {
                null, "b", "c", "d", "e", "f"
            }, new String[] {
                "a", "b", "c", "d", "e", null
            }
        };

        Fits     f    = new Fits();
        f.addHDU(Fits.makeHDU(data));
        BufferedFile ff = new BufferedFile("bt8.fits", "rw");
        f.write(ff);

        f = new Fits("bt8.fits");
        BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);

        assertEquals("deg21", "e", bhdu.getElement(4, data.length - 1));
        assertEquals("deg22", "", bhdu.getElement(5, data.length - 1));

        String[] col = (String[]) bhdu.getColumn(0);
        assertEquals("deg23", "a", col[0]);
        assertEquals("deg24", "f", col[5]);

        col = (String[]) bhdu.getColumn(3);
        assertEquals("deg25", "", col[0]);
        assertEquals("deg26", "", col[5]);

        col = (String[]) bhdu.getColumn(7);  // All nulls
        assertEquals("deg27", "", col[0]);
        assertEquals("deg28", "", col[5]);

        col = (String[]) bhdu.getColumn(8);

        assertEquals("deg29", "a", col[0]);
        assertEquals("deg210", "", col[1]);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testMultHDU() throws Exception {
        BufferedFile ff   = new BufferedFile("bt9.fits", "rw");
        Object[]     data = new Object[] {
            bytes, bits, bools, shorts, ints, floats, doubles, longs, strings
        };

        Fits         f    = new Fits();

        // Add two identical HDUs
        f.addHDU(Fits.makeHDU(data));
        f.addHDU(Fits.makeHDU(data));
        f.write(ff);
        ff.close();

        f = new Fits("bt9.fits");

        f.readHDU();
        BinaryTableHDU hdu;
        // This would fail before...
        int count = 0;
        while ((hdu = (BinaryTableHDU) f.readHDU()) != null) {
            int nrow = hdu.getHeader().getIntValue("NAXIS2");
            count += 1;
            assertEquals(nrow, 50);
            for (int i = 0; i < nrow; i += 1) {
                Object o = hdu.getRow(i);
            }
        }
        assertEquals(count, 2);
    }

    /**
     * _more_
     */
    @Test
    public void testByteArray() {
        String[] sarr   = { "abc", " de", "f" };
        byte[]   barr   = {
            'a', 'b', 'c', ' ', 'b', 'c', 'a', 'b', ' '
        };

        byte[]   obytes = nom.tam.fits.FitsUtil.stringsToByteArray(sarr, 3);
        assertEquals("blen", obytes.length, 9);
        assertEquals("b1", obytes[0], (byte) 'a');
        assertEquals("b1", obytes[1], (byte) 'b');
        assertEquals("b1", obytes[2], (byte) 'c');
        assertEquals("b1", obytes[3], (byte) ' ');
        assertEquals("b1", obytes[4], (byte) 'd');
        assertEquals("b1", obytes[5], (byte) 'e');
        assertEquals("b1", obytes[6], (byte) 'f');
        assertEquals("b1", obytes[7], (byte) ' ');
        assertEquals("b1", obytes[8], (byte) ' ');

        String[] ostrings = nom.tam.fits.FitsUtil.byteArrayToStrings(barr, 3);
        assertEquals("slen", ostrings.length, 3);
        assertEquals("s1", ostrings[0], "abc");
        assertEquals("s2", ostrings[1], "bc");
        assertEquals("s3", ostrings[2], "ab");
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void columnMetaTest() throws Exception {
        Object[] data = new Object[] { shorts, ints, floats, doubles };

        Fits     f    = new Fits();

        // Add two identical HDUs
        BinaryTableHDU bhdu = (BinaryTableHDU) Fits.makeHDU(data);
        f.addHDU(bhdu);

        // makeHDU creates the TFORM keywords and sometimes
        // the TDIM keywords.  Let's add some additional
        // column metadata.  For each column we'll want a TTYPE, TCOMM,
        // TUNIT and TX and TY
        // value and we want the final header to be in this order
        //     TTYPE, TCOMM, TFORM, [TDIM,] TUNIT, TX, TY
        int oldNCols = bhdu.getNCols();

        for (int i = 0; i < bhdu.getNCols(); i += 1) {
            bhdu.setColumnMeta(i, "TTYPE", "NAM" + (i + 1), null, false);
            bhdu.setColumnMeta(i, "TCOMM", true, "Comment in comment", false);
            bhdu.setColumnMeta(i, "TUNIT", "UNIT" + (i + 1), null, true);
            bhdu.setColumnMeta(i, "TX", (i + 1), null, true);
            bhdu.setColumnMeta(i, "TY", 2. * (i + 1), null, true);
        }

        BufferedFile ff = new BufferedFile("bt10.fits", "rw");
        f.write(ff);
        ff.close();
        f    = new Fits("bt10.fits");

        bhdu = (BinaryTableHDU) f.getHDU(1);
        Header hdr = bhdu.getHeader();
        assertEquals("metaCount", oldNCols, bhdu.getNCols());
        for (int i = 0; i < bhdu.getNCols(); i += 1) {
            // If this worked, the first header should be the TTYPE
            hdr.findCard("TTYPE" + (i + 1));
            HeaderCard hc = hdr.nextCard();
            assertEquals("M" + i + "0", "TTYPE" + (i + 1), hc.getKey());
            hc = hdr.nextCard();
            assertEquals("M" + i + "A", "TCOMM" + (i + 1), hc.getKey());
            hc = hdr.nextCard();
            assertEquals("M" + i + "B", "TFORM" + (i + 1), hc.getKey());
            hc = hdr.nextCard();
            // There may have been a TDIM keyword inserted automatically.  Let's
            // skip it if it was.  It should only appear immediately after the
            // TFORM keyword.
            if (hc.getKey().startsWith("TDIM")) {
                hc = hdr.nextCard();
            }
            assertEquals("M" + i + "C", "TUNIT" + (i + 1), hc.getKey());
            hc = hdr.nextCard();
            assertEquals("M" + i + "D", "TX" + (i + 1), hc.getKey());
            hc = hdr.nextCard();
            assertEquals("M" + i + "E", "TY" + (i + 1), hc.getKey());
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void specialStringsTest() throws Exception {
        String[] strings = new String[] {
            "abc", "abc\000", "abc\012abc", "abc\000abc", "abc\177",
            "abc\001def\002ghi\003"
        };

        String[] results1 = new String[] {
            strings[0], strings[0], strings[2], strings[0], strings[4],
            strings[5]
        };
        String[] results2 = new String[] {
            strings[0], strings[0], "abc abc", strings[0], "abc ",
            "abc def ghi "
        };

        FitsFactory.setUseAsciiTables(false);
        FitsFactory.setCheckAsciiStrings(false);

        Fits           f    = new Fits();

        Object[]       objs = new Object[] { strings };
        BinaryTableHDU bhdu = (BinaryTableHDU) Fits.makeHDU(objs);
        f.addHDU(bhdu);

        BufferedFile bf = new BufferedFile("bt11a.fits", "rw");
        f.write(bf);

        bf.close();

        f    = new Fits("bt11a.fits");
        bhdu = (BinaryTableHDU) f.getHDU(1);
        String[] vals = (String[]) bhdu.getColumn(0);
        for (int i = 0; i < strings.length; i += 1) {
            assertEquals("ssa" + i, results1[i], vals[i]);
        }

        FitsFactory.setCheckAsciiStrings(true);
        System.err.println(
            "  A warning about invalid ASCII strings should follow.");
        f    = new Fits();

        bhdu = (BinaryTableHDU) Fits.makeHDU(objs);
        f.addHDU(bhdu);
        bf = new BufferedFile("bt11b.fits", "rw");
        f.write(bf);

        bf.close();

        f    = new Fits("bt11b.fits");
        bhdu = (BinaryTableHDU) f.getHDU(1);
        vals = (String[]) bhdu.getColumn(0);
        for (int i = 0; i < strings.length; i += 1) {
            assertEquals("ssb" + i, results2[i], vals[i]);
        }

        FitsFactory.setCheckAsciiStrings(false);
    }
}
