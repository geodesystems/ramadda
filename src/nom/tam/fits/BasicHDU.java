
package nom.tam.fits;


import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;

/*
 * Copyright: Thomas McGlynn 1997-1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 *
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */
import java.io.IOException;

import java.util.Date;
import java.util.Iterator;


/**
 * This abstract class is the parent of all HDU types.
 * It provides basic functionality for an HDU.
 */
public abstract class BasicHDU implements FitsElement {

    /** _more_ */
    public static final int BITPIX_BYTE = 8;

    /** _more_ */
    public static final int BITPIX_SHORT = 16;

    /** _more_ */
    public static final int BITPIX_INT = 32;

    /** _more_ */
    public static final int BITPIX_LONG = 64;

    /** _more_ */
    public static final int BITPIX_FLOAT = -32;

    /** _more_ */
    public static final int BITPIX_DOUBLE = -64;

    /** The associated header. */
    protected Header myHeader = null;

    /** The associated data unit. */
    protected Data myData = null;

    /** Is this the first HDU in a FITS file? */
    protected boolean isPrimary = false;

    /**
     * Create a Data object to correspond to the header description.
     * @return An unfilled Data object which can be used to read
     *         in the data for this HDU.
     * @exception FitsException if the Data object could not be created
     *                          from this HDU's Header
     */
    abstract Data manufactureData() throws FitsException;

    /**
     * Skip the Data object immediately after the given Header object on
     * the given stream object.
     * @param stream the stream which contains the data.
     * @param hdr    template indicating length of Data section
     * @exception IOException if the Data object could not be skipped.
     */
    public static void skipData(ArrayDataInput stream, Header hdr)
            throws IOException {
        stream.skipBytes(hdr.getDataSize());
    }

    /**
     * Skip the Data object for this HDU.
     * @param stream the stream which contains the data.
     * @exception IOException if the Data object could not be skipped.
     */
    public void skipData(ArrayDataInput stream) throws IOException {
        skipData(stream, myHeader);
    }

    /**
     * Read in the Data object for this HDU.
     * @param stream the stream from which the data is read.
     * @exception FitsException if the Data object could not be created
     *                          from this HDU's Header
     */
    public void readData(ArrayDataInput stream) throws FitsException {
        myData = null;
        try {
            myData = manufactureData();
        } finally {
            // if we cannot build a Data object, skip this section
            if (myData == null) {
                try {
                    skipData(stream, myHeader);
                } catch (Exception e) {}
            }
        }

        myData.read(stream);
    }

    /**
     * Get the associated header
     *
     * @return _more_
     */
    public Header getHeader() {
        return myHeader;
    }

    /**
     * Get the starting offset of the HDU
     *
     * @return _more_
     */
    public long getFileOffset() {
        return myHeader.getFileOffset();
    }

    /**
     * Get the associated Data object
     *
     * @return _more_
     */
    public Data getData() {
        return myData;
    }

    /**
     * Get the non-FITS data object
     *
     * @return _more_
     */
    public Object getKernel() {
        try {
            return myData.getKernel();
        } catch (FitsException e) {
            return null;
        }
    }

    /**
     * Get the total size in bytes of the HDU.
     * @return The size in bytes.
     */
    public long getSize() {
        int size = 0;

        if (myHeader != null) {
            size += myHeader.getSize();
        }
        if (myData != null) {
            size += myData.getSize();
        }

        return size;
    }

    /**
     * Check that this is a valid header for the HDU.
     * @param header to validate.
     * @return <CODE>true</CODE> if this is a valid header.
     */
    public static boolean isHeader(Header header) {
        return false;
    }

    /**
     * Print out some information about this HDU.
     */
    public abstract void info();

    /**
     * Check if a field is present and if so print it out.
     * @param The header keyword.
     * @param Was it found in the header?
     *
     * @param name _more_
     *
     * @return _more_
     */
    boolean checkField(String name) {
        String value = myHeader.getStringValue(name);
        if (value == null) {
            return false;
        }

        return true;
    }

    /* Read out the HDU from the data stream.  This
     * will overwrite any existing header and data components.
     */

    /**
     * _more_
     *
     * @param stream _more_
     *
     * @throws FitsException _more_
     * @throws IOException _more_
     */
    public void read(ArrayDataInput stream)
            throws FitsException, IOException {
        myHeader = Header.readHeader(stream);
        myData   = myHeader.makeData();
        myData.read(stream);
    }

    /* Write out the HDU
     * @param stream The data stream to be written to.
     */

    /**
     * _more_
     *
     * @param stream _more_
     *
     * @throws FitsException _more_
     */
    public void write(ArrayDataOutput stream) throws FitsException {
        if (myHeader != null) {
            myHeader.write(stream);
        }
        if (myData != null) {
            myData.write(stream);
        }
        try {
            stream.flush();
        } catch (java.io.IOException e) {
            throw new FitsException("Error flushing at end of HDU: "
                                    + e.getMessage());
        }
    }

    /**
     * Is the HDU rewriteable
     *
     * @return _more_
     */
    public boolean rewriteable() {
        return myHeader.rewriteable() && myData.rewriteable();
    }

    /**
     * Rewrite the HDU
     *
     * @throws FitsException _more_
     * @throws IOException _more_
     */
    public void rewrite() throws FitsException, IOException {

        if (rewriteable()) {
            myHeader.rewrite();
            myData.rewrite();
        } else {
            throw new FitsException("Invalid attempt to rewrite HDU");
        }
    }

    /**
     * Get the String value associated with <CODE>keyword</CODE>.
     * @param keyword   the FITS keyword
     * @return  either <CODE>null</CODE> or a String with leading/trailing
     *          blanks stripped.
     */
    public String getTrimmedString(String keyword) {
        String s = myHeader.getStringValue(keyword);
        if (s != null) {
            s = s.trim();
        }

        return s;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public int getBitPix() throws FitsException {
        int bitpix = myHeader.getIntValue("BITPIX", -1);
        switch (bitpix) {

          case BITPIX_BYTE :
          case BITPIX_SHORT :
          case BITPIX_INT :
          case BITPIX_FLOAT :
          case BITPIX_DOUBLE :
              break;

          default :
              throw new FitsException("Unknown BITPIX type " + bitpix);
        }

        return bitpix;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public int[] getAxes() throws FitsException {
        int nAxis = myHeader.getIntValue("NAXIS", 0);
        if (nAxis < 0) {
            throw new FitsException("Negative NAXIS value " + nAxis);
        }
        if (nAxis > 999) {
            throw new FitsException("NAXIS value " + nAxis + " too large");
        }

        if (nAxis == 0) {
            return null;
        }

        int[] axes = new int[nAxis];
        for (int i = 1; i <= nAxis; i++) {
            axes[nAxis - i] = myHeader.getIntValue("NAXIS" + i, 0);
        }

        return axes;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getParameterCount() {
        return myHeader.getIntValue("PCOUNT", 0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getGroupCount() {
        return myHeader.getIntValue("GCOUNT", 1);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getBScale() {
        return myHeader.getDoubleValue("BSCALE", 1.0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getBZero() {
        return myHeader.getDoubleValue("BZERO", 0.0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getBUnit() {
        return getTrimmedString("BUNIT");
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public int getBlankValue() throws FitsException {
        if ( !myHeader.containsKey("BLANK")) {
            throw new FitsException("BLANK undefined");
        }

        return myHeader.getIntValue("BLANK");
    }

    /**
     * Get the FITS file creation date as a <CODE>Date</CODE> object.
     * @return  either <CODE>null</CODE> or a Date object
     */
    public Date getCreationDate() {
        try {
            return new FitsDate(myHeader.getStringValue("DATE")).toDate();
        } catch (FitsException e) {
            return null;
        }
    }

    /**
     * Get the FITS file observation date as a <CODE>Date</CODE> object.
     * @return  either <CODE>null</CODE> or a Date object
     */
    public Date getObservationDate() {
        try {
            return new FitsDate(myHeader.getStringValue("DATE-OBS")).toDate();
        } catch (FitsException e) {
            return null;
        }
    }

    /**
     * Get the name of the organization which created this FITS file.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getOrigin() {
        return getTrimmedString("ORIGIN");
    }

    /**
     * Get the name of the telescope which was used to acquire the data in
     * this FITS file.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getTelescope() {
        return getTrimmedString("TELESCOP");
    }

    /**
     * Get the name of the instrument which was used to acquire the data in
     * this FITS file.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getInstrument() {
        return getTrimmedString("INSTRUME");
    }

    /**
     * Get the name of the person who acquired the data in this FITS file.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getObserver() {
        return getTrimmedString("OBSERVER");
    }

    /**
     * Get the name of the observed object in this FITS file.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getObject() {
        return getTrimmedString("OBJECT");
    }

    /**
     * Get the equinox in years for the celestial coordinate system in which
     * positions given in either the header or data are expressed.
     * @return  either <CODE>null</CODE> or a String object
     */
    public double getEquinox() {
        return myHeader.getDoubleValue("EQUINOX", -1.0);
    }

    /**
     * Get the equinox in years for the celestial coordinate system in which
     * positions given in either the header or data are expressed.
     * @return  either <CODE>null</CODE> or a String object
     * @deprecated      Replaced by getEquinox
     * @see     #getEquinox()
     */
    public double getEpoch() {
        return myHeader.getDoubleValue("EPOCH", -1.0);
    }

    /**
     * Return the name of the person who compiled the information in
     * the data associated with this header.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getAuthor() {
        return getTrimmedString("AUTHOR");
    }

    /**
     * Return the citation of a reference where the data associated with
     * this header are published.
     * @return  either <CODE>null</CODE> or a String object
     */
    public String getReference() {
        return getTrimmedString("REFERENC");
    }

    /**
     * Return the minimum valid value in the array.
     * @return  minimum value.
     */
    public double getMaximumValue() {
        return myHeader.getDoubleValue("DATAMAX");
    }

    /**
     * Return the minimum valid value in the array.
     * @return  minimum value.
     */
    public double getMinimumValue() {
        return myHeader.getDoubleValue("DATAMIN");
    }

    /**
     * Indicate whether HDU can be primary HDU.
     *  This method must be overriden in HDU types which can
     *  appear at the beginning of a FITS file.
     *
     * @return _more_
     */
    boolean canBePrimary() {
        return false;
    }

    /**
     * Reset the input stream to the beginning of the HDU, i.e., the beginning of the header
     *
     * @return _more_
     */
    public boolean reset() {
        return myHeader.reset();
    }

    /**
     * Indicate that an HDU is the first element of a FITS file.
     *
     * @param newPrimary _more_
     *
     * @throws FitsException _more_
     */
    void setPrimaryHDU(boolean newPrimary) throws FitsException {

        if (newPrimary && !canBePrimary()) {
            throw new FitsException("Invalid attempt to make HDU of type:"
                                    + this.getClass().getName()
                                    + " primary.");
        } else {
            this.isPrimary = newPrimary;
        }

        // Some FITS readers don't like the PCOUNT and GCOUNT keywords
        // in a primary array or they EXTEND keyword in extensions.

        if (isPrimary && !myHeader.getBooleanValue("GROUPS", false)) {
            myHeader.deleteKey("PCOUNT");
            myHeader.deleteKey("GCOUNT");
        }

        if (isPrimary) {
            HeaderCard card = myHeader.findCard("EXTEND");
            if (card == null) {
                getAxes();  // Leaves the iterator pointing to the last NAXISn card.
                myHeader.nextCard();
                myHeader.addValue("EXTEND", true, "ntf::basichdu:extend:1");
            }
        }

        if ( !isPrimary) {

            Iterator iter   = myHeader.iterator();

            int      pcount = myHeader.getIntValue("PCOUNT", 0);
            int      gcount = myHeader.getIntValue("GCOUNT", 1);
            int      naxis  = myHeader.getIntValue("NAXIS", 0);
            myHeader.deleteKey("EXTEND");
            HeaderCard card;
            HeaderCard pcard = myHeader.findCard("PCOUNT");
            HeaderCard gcard = myHeader.findCard("GCOUNT");

            myHeader.getCard(2 + naxis);
            if (pcard == null) {
                myHeader.addValue("PCOUNT", pcount, "ntf::basichdu:pcount:1");
            }
            if (gcard == null) {
                myHeader.addValue("GCOUNT", gcount, "ntf::basichdu:gcount:1");
            }
            iter = myHeader.iterator();
        }

    }

    /**
     * Add information to the header
     *
     * @param key _more_
     * @param val _more_
     * @param comment _more_
     *
     * @throws HeaderCardException _more_
     */
    public void addValue(String key, boolean val, String comment)
            throws HeaderCardException {
        myHeader.addValue(key, val, comment);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param val _more_
     * @param comment _more_
     *
     * @throws HeaderCardException _more_
     */
    public void addValue(String key, int val, String comment)
            throws HeaderCardException {
        myHeader.addValue(key, val, comment);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param val _more_
     * @param comment _more_
     *
     * @throws HeaderCardException _more_
     */
    public void addValue(String key, double val, String comment)
            throws HeaderCardException {
        myHeader.addValue(key, val, comment);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param val _more_
     * @param comment _more_
     *
     * @throws HeaderCardException _more_
     */
    public void addValue(String key, String val, String comment)
            throws HeaderCardException {
        myHeader.addValue(key, val, comment);
    }

    /**
     * Get an HDU without content
     *
     * @return _more_
     */
    public static BasicHDU getDummyHDU() {
        try {
            // Update suggested by Laurent Bourges
            ImageData img = new ImageData((Object) null);

            return FitsFactory.HDUFactory(ImageHDU.manufactureHeader(img),
                                          img);
        } catch (FitsException e) {
            System.err.println("Impossible exception in getDummyHDU");

            return null;
        }
    }
}
