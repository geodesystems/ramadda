
package nom.tam.fits.test;


import junit.framework.JUnit4TestAdapter;

import nom.tam.fits.*;

import nom.tam.image.*;
import nom.tam.util.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;


/**
 * Test reading .Z and .gz compressed files.
 */
public class CompressTest {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testgz() throws Exception {

        File fil = new File(".");
        System.out.println("File is:" + fil.getCanonicalPath());
        Fits f =
            new Fits(
                "http://heasarc.gsfc.nasa.gov/FTP/asca/data/rev2/43021000/images/ad43021000gis25670_lo.totsky.gz");

        BasicHDU h    = f.readHDU();
        int[][]  data = (int[][]) h.getKernel();
        double   sum  = 0;
        for (int i = 0; i < data.length; i += 1) {
            for (int j = 0; j < data[i].length; j += 1) {
                sum += data[i][j];
            }
        }
        assertEquals("ZCompress", sum, 296915., 0);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testZ() throws Exception {

        Fits f =
            new Fits(
                "http://heasarc.gsfc.nasa.gov/FTP/rosat/data/pspc/processed_data/600000/rp600245n00/rp600245n00_im1.fits.Z");

        BasicHDU  h    = f.readHDU();
        short[][] data = (short[][]) h.getKernel();
        double    sum  = 0;
        for (int i = 0; i < data.length; i += 1) {
            for (int j = 0; j < data[i].length; j += 1) {
                sum += data[i][j];
            }
        }
        assertEquals("ZCompress", sum, 91806., 0);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testStream() throws Exception {
        InputStream is;

        is = new FileInputStream("test.fits");
        assertEquals("Stream1", 300, streamRead(is, false, false));

        is = new FileInputStream("test.fits.Z");
        assertEquals("Stream2", 300, streamRead(is, false, false));

        is = new FileInputStream("test.fits.gz");
        assertEquals("Stream3", 300, streamRead(is, false, false));

        is = new FileInputStream("test.fits");
        assertEquals("Stream4", 300, streamRead(is, false, true));

        is = new FileInputStream("test.fits.Z");
        assertEquals("Stream5", 300, streamRead(is, false, true));

        is = new FileInputStream("test.fits.gz");
        assertEquals("Stream6", 300, streamRead(is, false, true));


        is = new FileInputStream("test.fits.Z");
        assertEquals("Stream7", 300, streamRead(is, true, true));

        is = new FileInputStream("test.fits.gz");
        assertEquals("Stream8", 300, streamRead(is, true, true));

        is = new FileInputStream("test.fits.bz2");
        assertEquals("Stream9", 300, streamRead(is, true, true));
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testFile() throws Exception {
        File is = new File("test.fits");
        assertEquals("File1", 300, fileRead(is, false, false));

        is = new File("test.fits.Z");
        assertEquals("File2", 300, fileRead(is, false, false));

        is = new File("test.fits.gz");
        assertEquals("File3", 300, fileRead(is, false, false));

        is = new File("test.fits");
        assertEquals("File4", 300, fileRead(is, false, true));

        is = new File("test.fits.Z");
        assertEquals("File7", 300, fileRead(is, true, true));

        is = new File("test.fits.gz");
        assertEquals("File8", 300, fileRead(is, true, true));

        is = new File("test.fits.bz2");
        assertEquals("File9", 300, fileRead(is, true, true));
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testString() throws Exception {
        String is = "test.fits";
        assertEquals("String1", 300, stringRead(is, false, false));

        is = "test.fits.Z";
        assertEquals("String2", 300, stringRead(is, false, false));

        is = "test.fits.gz";
        assertEquals("String3", 300, stringRead(is, false, false));

        is = "test.fits";
        assertEquals("String4", 300, stringRead(is, false, true));

        is = "test.fits.Z";
        assertEquals("String7", 300, stringRead(is, true, true));

        is = "test.fits.gz";
        assertEquals("String8", 300, stringRead(is, true, true));

        is = "test.fits.bz2";
        assertEquals("String8", 300, stringRead(is, true, true));

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testURL() throws Exception {
        String is = "test.fits";
        assertEquals("String1", 300, urlRead(is, false, false));

        is = "test.fits.Z";
        assertEquals("String2", 300, urlRead(is, false, false));

        is = "test.fits.gz";
        assertEquals("String3", 300, urlRead(is, false, false));

        is = "test.fits";
        assertEquals("String4", 300, urlRead(is, false, true));

        is = "test.fits.Z";
        assertEquals("String7", 300, urlRead(is, true, true));

        is = "test.fits.gz";
        assertEquals("String8", 300, urlRead(is, true, true));

        is = "test.fits.bz2";
        assertEquals("String8", 300, urlRead(is, true, true));
    }

    /**
     * _more_
     *
     * @param is _more_
     * @param comp _more_
     * @param useComp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    int urlRead(String is, boolean comp, boolean useComp) throws Exception {
        File   fil  = new File(is);

        String path = fil.getCanonicalPath();
        URL    u    = new URL("file://" + path);

        Fits   f;
        if (useComp) {
            f = new Fits(u, comp);
        } else {
            f = new Fits(u);
        }
        short[][] data = (short[][]) f.readHDU().getKernel();

        return total(data);
    }

    /**
     * _more_
     *
     * @param is _more_
     * @param comp _more_
     * @param useComp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    int streamRead(InputStream is, boolean comp, boolean useComp)
            throws Exception {
        Fits f;
        if (useComp) {
            f = new Fits(is, comp);
        } else {
            f = new Fits(is);
        }
        short[][] data = (short[][]) f.readHDU().getKernel();
        is.close();

        return total(data);
    }

    /**
     * _more_
     *
     * @param is _more_
     * @param comp _more_
     * @param useComp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    int fileRead(File is, boolean comp, boolean useComp) throws Exception {
        Fits f;
        if (useComp) {
            f = new Fits(is, comp);
        } else {
            f = new Fits(is);
        }
        short[][] data = (short[][]) f.readHDU().getKernel();

        return total(data);
    }

    /**
     * _more_
     *
     * @param is _more_
     * @param comp _more_
     * @param useComp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    int stringRead(String is, boolean comp, boolean useComp)
            throws Exception {
        Fits f;
        if (useComp) {
            f = new Fits(is, comp);
        } else {
            f = new Fits(is);
        }
        short[][] data = (short[][]) f.readHDU().getKernel();

        return total(data);
    }

    /**
     * _more_
     *
     * @param data _more_
     *
     * @return _more_
     */
    int total(short[][] data) {
        int total = 0;
        for (int i = 0; i < data.length; i += 1) {
            for (int j = 0; j < data[i].length; j += 1) {
                total += data[i][j];
            }
        }

        return total;
    }
}
