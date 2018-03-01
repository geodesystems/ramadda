
package nom.tam.fits;


import nom.tam.util.*;

/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */
import java.io.*;


/**
 * This class provides methods to access the data segment of an
 * HDU.
 */
public abstract class Data implements FitsElement {

    /**
     * This is the object which contains the actual data for the HDU.
     * <ul>
     *  <li> For images and primary data this is a simple (but possibly
     *       multi-dimensional) primitive array.  When group data is
     *       supported it will be a possibly multidimensional array
     *       of group objects.
     *  <li> For ASCII data it is a two dimensional Object array where
     *       each of the constituent objects is a primitive array of length 1.
     *  <li> For Binary data it is a two dimensional Object array where
     *       each of the constituent objects is a primitive array of arbitrary
     *       (more or less) dimensionality.
     *  </ul>
     */

    /** The starting location of the data when last read */
    protected long fileOffset = -1;

    /** The size of the data when last read */
    protected long dataSize;

    /** The inputstream used. */
    protected RandomAccess input;

    /**
     * Get the file offset
     *
     * @return _more_
     */
    public long getFileOffset() {
        return fileOffset;
    }

    /**
     * Set the fields needed for a re-read
     *
     * @param o _more_
     */
    protected void setFileOffset(Object o) {
        if (o instanceof RandomAccess) {
            fileOffset = FitsUtil.findOffset(o);
            dataSize   = getTrueSize();
            input      = (RandomAccess) o;
        }
    }

    /**
     * Write the data -- including any buffering needed
     * @param o  The output stream on which to write the data.
     *
     * @throws FitsException _more_
     */
    public abstract void write(ArrayDataOutput o) throws FitsException;

    /**
     * Read a data array into the current object and if needed position
     * to the beginning of the next FITS block.
     * @param i The input data stream
     *
     * @throws FitsException _more_
     */
    public abstract void read(ArrayDataInput i) throws FitsException;

    /**
     * _more_
     *
     * @throws FitsException _more_
     */
    public void rewrite() throws FitsException {

        if ( !rewriteable()) {
            throw new FitsException("Illegal attempt to rewrite data");
        }

        FitsUtil.reposition(input, fileOffset);
        write((ArrayDataOutput) input);
        try {
            ((ArrayDataOutput) input).flush();
        } catch (IOException e) {
            throw new FitsException("Error in rewrite flush: " + e);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean reset() {
        try {
            FitsUtil.reposition(input, fileOffset);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean rewriteable() {
        if ((input == null) || (fileOffset < 0)
                || (getTrueSize() + 2879) / 2880
                   != (dataSize + 2879) / 2880) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    abstract long getTrueSize();

    /**
     * Get the size of the data element in bytes
     *
     * @return _more_
     */
    public long getSize() {
        return FitsUtil.addPadding(getTrueSize());
    }

    /**
     * Return the data array object.
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public abstract Object getData() throws FitsException;

    /**
     * Return the non-FITS data object
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public Object getKernel() throws FitsException {
        return getData();
    }

    /**
     * Modify a header to point to this data
     *
     * @param head _more_
     *
     * @throws FitsException _more_
     */
    abstract void fillHeader(Header head) throws FitsException;
}
