
package nom.tam.util.test;


import junit.framework.JUnit4TestAdapter;

import nom.tam.util.BufferedDataInputStream;

import nom.tam.util.BufferedFile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
public class BigFileTest {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Test
    public void test() throws Exception {
        try {
            // First create a 3 GB file.
            String fname = System.getenv("BIGFILETEST");
            if (fname == null) {
                System.out.println(
                    "BIGFILETEST environment not set.  Returning without test");

                return;
            }
            System.out.println("Big file test.  Takes quite a while.");
            byte[]       buf    = new byte[100000000];  // 100 MB
            BufferedFile bf     = new BufferedFile(fname, "rw");
            byte         sample = 13;

            for (int i = 0; i < 30; i += 1) {
                bf.write(buf);                          // 30 x 100 MB = 3 GB.
                if (i == 24) {
                    bf.write(new byte[] { sample });
                }                                       // Add a marker.
            }
            bf.close();

            // Now try to skip within the file.
            bf = new BufferedFile(fname, "r");
            long skip = 2500000000L;  // 2.5 G

            long val1 = bf.skipBytes(skip);
            long val2 = bf.getFilePointer();
            int  val  = bf.read();
            bf.close();

            assertEquals("SkipResult", skip, val1);
            assertEquals("SkipPos", skip, val2);
            assertEquals("SkipVal", (int) sample, val);

            BufferedDataInputStream bdis =
                new BufferedDataInputStream(new FileInputStream(fname));
            val1 = bdis.skipBytes(skip);
            val  = bdis.read();
            bdis.close();
            assertEquals("SSkipResult", skip, val1);
            assertEquals("SSkipVal", (int) sample, val);
        } catch (Exception e) {
            e.printStackTrace(System.err);

            throw e;
        }
    }
}
