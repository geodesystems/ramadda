
package nom.tam.fits;
/* Copyright: Thomas McGlynn 1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */


import nom.tam.util.*;

import java.io.EOFException;
import java.io.IOException;


/**
 * This class instantiates FITS Random Groups data.
 *  Random groups are instantiated as a two-dimensional
 *  array of objects.  The first dimension of the array
 *  is the number of groups.  The second dimension is 2.
 *  The first object in every row is a one dimensional
 *  parameter array.  The second element is the n-dimensional
 *  data array.
 */
public class RandomGroupsData extends Data {

    /** _more_ */
    private Object[][] dataArray;

    /**
     * Create the equivalent of a null data element.
     */
    public RandomGroupsData() {
        dataArray = new Object[0][];
    }

    /**
     * Create a RandomGroupsData object using the specified object to
     *  initialize the data array.
     *  @param x The initial data array.  This should a two-d
     *           array of objects as described above.
     */
    public RandomGroupsData(Object[][] x) {
        dataArray = x;
    }

    /**
     * Get the size of the actual data element.
     *
     * @return _more_
     */
    protected long getTrueSize() {

        if ((dataArray != null) && (dataArray.length > 0)) {
            return (ArrayFuncs.computeLSize(dataArray[0][0])
                    + ArrayFuncs.computeLSize(
                        dataArray[0][1])) * dataArray.length;
        } else {
            return 0;
        }
    }

    /**
     * Read the RandomGroupsData
     *
     * @param str _more_
     *
     * @throws FitsException _more_
     */
    public void read(ArrayDataInput str) throws FitsException {

        setFileOffset(str);

        try {
            str.readLArray(dataArray);
        } catch (IOException e) {
            throw new FitsException("IO error reading Random Groups data "
                                    + e);
        }
        int pad = FitsUtil.padding(getTrueSize());
        try {
            str.skipBytes(pad);
        } catch (EOFException e) {
            throw new PaddingException(
                "EOF reading padding after random groups", this);
        } catch (IOException e) {
            throw new FitsException(
                "IO error reading padding after random groups");
        }
    }

    /**
     * Write the RandomGroupsData
     *
     * @param str _more_
     *
     * @throws FitsException _more_
     */
    public void write(ArrayDataOutput str) throws FitsException {
        try {
            str.writeArray(dataArray);
            FitsUtil.pad(str, getTrueSize());
        } catch (IOException e) {
            throw new FitsException("IO error writing random groups data "
                                    + e);
        }
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @throws FitsException _more_
     */
    protected void fillHeader(Header h) throws FitsException {

        if ((dataArray.length <= 0) || (dataArray[0].length != 2)) {
            throw new FitsException("Data not conformable to Random Groups");
        }

        int    gcount   = dataArray.length;
        Object paraSamp = dataArray[0][0];
        Object dataSamp = dataArray[0][1];

        Class  pbase    = nom.tam.util.ArrayFuncs.getBaseClass(paraSamp);
        Class  dbase    = nom.tam.util.ArrayFuncs.getBaseClass(dataSamp);

        if (pbase != dbase) {
            throw new FitsException(
                "Data and parameters do not agree in type for random group");
        }

        int[] pdims = nom.tam.util.ArrayFuncs.getDimensions(paraSamp);
        int[] ddims = nom.tam.util.ArrayFuncs.getDimensions(dataSamp);

        if (pdims.length != 1) {
            throw new FitsException(
                "Parameters are not 1 d array for random groups");
        }

        // Got the information we need to build the header.

        h.setSimple(true);
        if (dbase == byte.class) {
            h.setBitpix(8);
        } else if (dbase == short.class) {
            h.setBitpix(16);
        } else if (dbase == int.class) {
            h.setBitpix(32);
        } else if (dbase == long.class) {  // Non-standard
            h.setBitpix(64);
        } else if (dbase == float.class) {
            h.setBitpix(-32);
        } else if (dbase == double.class) {
            h.setBitpix(-64);
        } else {
            throw new FitsException("Data type:" + dbase
                                    + " not supported for random groups");
        }


        h.setNaxes(ddims.length + 1);
        h.addValue("NAXIS1", 0, "ntf::randomgroupsdata:naxis1:1");
        for (int i = 2; i <= ddims.length + 1; i += 1) {
            h.addValue("NAXIS" + i, ddims[i - 2],
                       "ntf::randomgroupsdata:naxisN:1");
        }

        h.addValue("GROUPS", true, "ntf::randomgroupsdata:groups:1");
        h.addValue("GCOUNT", dataArray.length,
                   "ntf::randomgroupsdata:gcount:1");
        h.addValue("PCOUNT", pdims[0], "ntf::randomgroupsdata:pcount:1");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object getData() {
        return dataArray;
    }


}
