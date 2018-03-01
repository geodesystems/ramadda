
package nom.tam.util;


import java.io.IOException;


/**
 * Interface description
 *
 *
 * @author         Enter your name here...
 */
public interface ArrayDataInput extends java.io.DataInput {

    /**
     * Read a generic (possibly multidimenionsional) primitive array.
     * An  Object[] array is also a legal argument if each element
     * of the array is a legal.
     * <p>
     * The ArrayDataInput classes do not support String input since
     * it is unclear how one would read in an Array of strings.
     * @param o   A [multidimensional] primitive (or Object) array.
     * @deprecated See readLArray(Object o).
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int readArray(Object o) throws IOException;

    /**
     * Read an array. This version works even if the
     * underlying data is more than 2 Gigabytes.
     *
     * @param o _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public long readLArray(Object o) throws IOException;

    /* Read a complete primitive array */

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(byte[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(boolean[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(short[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(char[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(int[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(long[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(float[] buf) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(double[] buf) throws IOException;

    /* Read a segment of a primitive array. */

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(byte[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(boolean[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(char[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(short[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(int[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(long[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(float[] buf, int offset, int size) throws IOException;

    /**
     * _more_
     *
     * @param buf _more_
     * @param offset _more_
     * @param size _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int read(double[] buf, int offset, int size) throws IOException;

    /* Skip (forward) in a file */

    /**
     * _more_
     *
     * @param distance _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public long skip(long distance) throws IOException;

    /* Skip and require that the data be there. */

    /**
     * _more_
     *
     * @param toSkip _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public long skipBytes(long toSkip) throws IOException;

    /* Close the file. */

    /**
     * _more_
     *
     * @throws IOException _more_
     */
    public void close() throws IOException;
}
