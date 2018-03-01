
package nom.tam.util;


import java.io.IOException;


/**
 * Interface description
 *
 *
 * @author         Enter your name here...
 */
public interface ArrayDataOutput extends java.io.DataOutput {

    /**
     * Write a generic (possibly multi-dimenionsional) primitive or String
     *  array.  An array of Objects is also allowed if all
     *  of the elements are valid arrays.
     *  <p>
     *  This routine is not called 'write' to avoid possible compilation
     *  errors in routines which define only some of the other methods
     *  of the interface (and defer to the superclass on others).
     *  In that case there is an ambiguity as to whether to
     *  call the routine in the current class but convert to
     *  Object, or call the method from the super class with
     *  the same type argument.
     *  @param o        The primitive or String array to be written.
     *  @throws IOException if the argument is not of the proper type
     */
    public void writeArray(Object o) throws IOException;

    /* Write a complete array */

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(byte[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(boolean[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(short[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(char[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(int[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(long[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(float[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(double[] buf) throws IOException;

    /* Write an array of Strings */

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @throws IOException _more_
     */
    public void write(String[] buf) throws IOException;

    /* Write a segment of a primitive array. */

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(byte[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(boolean[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(char[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(short[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(int[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(long[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(float[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(double[] buf, int offset, int size) throws IOException;

    /* Write some of an array of Strings */

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @throws IOException _more_
     */
    public void write(String[] buf, int offset, int size) throws IOException;

    /* Flush the output buffer */

    /**
     * _more_
     *
     * @throws IOException _more_
     */
    public void flush() throws IOException;

    /**
     * _more_
     *
     * @throws IOException _more_
     */
    public void close() throws IOException;
}
