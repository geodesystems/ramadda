
/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */

package nom.tam.fits;


import nom.tam.util.*;

import java.io.IOException;


/**
 * Interface description
 *
 *
 * @author         Enter your name here...
 */
public interface FitsElement {

    /**
     * Read the contents of the element from an input source.
     *  @param in       The input source.
     *
     * @throws FitsException _more_
     * @throws IOException _more_
     */
    public void read(ArrayDataInput in) throws FitsException, IOException;

    /**
     * Write the contents of the element to a data sink.
     *  @param out      The data sink.
     *
     * @throws FitsException _more_
     * @throws IOException _more_
     */
    public void write(ArrayDataOutput out) throws FitsException, IOException;

    /**
     * Rewrite the contents of the element in place.
     *  The data must have been orignally read from a random
     *  access device, and the size of the element may not have changed.
     *
     * @throws FitsException _more_
     * @throws IOException _more_
     */
    public void rewrite() throws FitsException, IOException;

    /**
     * Get the byte at which this element begins.
     *  This is only available if the data is originally read from
     *  a random access medium.
     *
     * @return _more_
     */
    public long getFileOffset();

    /**
     * Can this element be rewritten?
     *
     * @return _more_
     */
    public boolean rewriteable();

    /**
     * The size of this element in bytes
     *
     * @return _more_
     */
    public long getSize();

    /**
     * Reset the input stream to point to the beginning of this element
     * @return True if the reset succeeded.
     */
    public boolean reset();
}
