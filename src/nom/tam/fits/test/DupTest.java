
package nom.tam.fits.test;


import junit.framework.JUnit4TestAdapter;

import nom.tam.fits.*;

import nom.tam.image.*;
import nom.tam.util.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import java.util.List;


/**
 * Test adding a little junk after a valid image.
 *  We wish to test three scenarios:
 *     Junk at the beginning (should continue to fail)
 *     Short (<80 byte) junk after valid HDU
 *     Long  (>80 byte) junk after valid HDU
 *  The last two should succeed after FitsFactory.setAllowTerminalJunk(true).
 */
public class DupTest {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void test() throws Exception {

        Fits   f   = new Fits("test_dup.fits");
        Header hdr = f.readHDU().getHeader();
        assertEquals("Internal size:", hdr.getSize(), 2880);
        assertEquals("External size:", hdr.getOriginalSize(), 8640);
        assertTrue("Has duplicates:", hdr.hadDuplicates());
        List<HeaderCard> dups = hdr.getDuplicates();
        System.out.println("Number of duplicates:" + dups.size());
        assertTrue("Has dups:", (dups != null) && (dups.size() > 0));
        assertTrue("Not rewriteable:", !hdr.rewriteable());
        BufferedFile bf = new BufferedFile("created_dup.fits", "rw");
        f.write(bf);
        hdr.resetOriginalSize();
        assertEquals("External size, after reset", hdr.getOriginalSize(),
                     2880);
        Fits g = new Fits("created_dup.fits");
        hdr = g.readHDU().getHeader();
        assertEquals("Internal size, after rewrite", hdr.getSize(), 2880);
        assertEquals("External size, after rewrite", hdr.getOriginalSize(),
                     2880);
        assertTrue("Now rewriteable", hdr.rewriteable());
        assertTrue("No duplicates", !hdr.hadDuplicates());
        assertTrue("Dups is null", hdr.getDuplicates() == null);
    }
}
