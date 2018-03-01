/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nom.tam.fits.test;


import junit.framework.JUnit4TestAdapter;

import nom.tam.fits.*;
import nom.tam.util.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.*;


/**
 *
 * @author tmcglynn
 */
public class ChecksumTest {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void testChecksum() throws Exception {

        int[][]  data = new int[][] {
            { 1, 2 }, { 3, 4 }, { 5, 6 }
        };
        Fits     f    = new Fits();
        BasicHDU bhdu = FitsFactory.HDUFactory(data);
        f.addHDU(bhdu);

        Fits.setChecksum(bhdu);
        ByteArrayOutputStream    bs   = new ByteArrayOutputStream();
        BufferedDataOutputStream bdos = new BufferedDataOutputStream(bs);
        f.write(bdos);
        bdos.close();
        byte[] stream = bs.toByteArray();
        long   chk    = Fits.checksum(stream);
        int    val    = (int) chk;

        assertEquals("CheckSum test", -1, val);
    }

}
