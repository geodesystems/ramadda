
package nom.tam.fits.test;


import junit.framework.JUnit4TestAdapter;

import nom.tam.fits.*;

import nom.tam.image.*;
import nom.tam.util.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;


/**
 * Test adding a little junk after a valid image.
 *  We wish to test three scenarios:
 *     Junk at the beginning (should continue to fail)
 *     Short (<80 byte) junk after valid HDU
 *     Long  (>80 byte) junk after valid HDU
 *  The last two should succeed after FitsFactory.setAllowTerminalJunk(true).
 */
public class JunkTest {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void test() throws Exception {

        Fits   f    = new Fits();

        byte[] bimg = new byte[40];
        for (int i = 10; i < bimg.length; i += 1) {
            bimg[i] = (byte) i;
        }

        // Make HDUs of various types.
        f.addHDU(Fits.makeHDU(bimg));


        // Write a FITS file.

        // Valid FITS with one HDU
        BufferedFile bf = new BufferedFile("j1.fits", "rw");
        f.write(bf);
        bf.flush();
        bf.close();

        // Invalid junk with no valid FITS.
        bf = new BufferedFile("j2.fits", "rw");
        bf.write(new byte[10]);
        bf.close();

        // Valid FITS followed by short junk.
        bf = new BufferedFile("j3.fits", "rw");
        f.write(bf);
        bf.write("JUNKJUNK".getBytes());
        bf.close();

        // Valid FITS followed by long junk.
        bf = new BufferedFile("j4.fits", "rw");
        f.write(bf);
        for (int i = 0; i < 100; i += 1) {
            bf.write("A random string".getBytes());
        }
        bf.close();

        int pos = 0;
        try {
            f = new Fits("j1.fits");
            f.read();
        } catch (Exception e) {
            pos = 1;
        }
        assertTrue("Junk Test: Valid File OK,Dft", readSuccess("j1.fits"));
        assertTrue("Junk Test: Invalid File Fails, Dft",
                   !readSuccess("j2.fits"));
        assertTrue("Junk Test: Short junk fails, Dft",
                   !readSuccess("j3.fits"));
        assertTrue("Junk Test: Long junk fails, Dft",
                   !readSuccess("j4.fits"));

        FitsFactory.setAllowTerminalJunk(true);

        assertTrue("Junk Test: Valid File OK,with junk",
                   readSuccess("j1.fits"));
        assertTrue("Junk Test: Invalid File Fails, with junk",
                   !readSuccess("j2.fits"));
        assertTrue("Junk Test: Short junk OK, with junk",
                   readSuccess("j3.fits"));
        assertTrue("Junk Test: Long junk OK, with junk",
                   readSuccess("j4.fits"));

        FitsFactory.setAllowTerminalJunk(false);

        assertTrue("Junk Test: Valid File OK,No junk",
                   readSuccess("j1.fits"));
        assertTrue("Junk Test: Invalid File Fails, No junk",
                   !readSuccess("j2.fits"));
        assertTrue("Junk Test: Short junk fails, No junk",
                   !readSuccess("j3.fits"));
        assertTrue("Junk Test: Long junk fails, No junk",
                   !readSuccess("j4.fits"));
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    boolean readSuccess(String file) {
        try {
            Fits f = new Fits(file);
            f.read();

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
