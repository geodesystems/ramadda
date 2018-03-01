
package nom.tam.fits;


/**
 * This class allows FITS binary and ASCII tables to
 *  be accessed via a common interface.
 */

public interface TableData {

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public abstract Object[] getRow(int row) throws FitsException;

    /**
     * _more_
     *
     * @param col _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public abstract Object getColumn(int col) throws FitsException;

    /**
     * _more_
     *
     * @param row _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public abstract Object getElement(int row, int col) throws FitsException;

    /**
     * _more_
     *
     * @param row _more_
     * @param newRow _more_
     *
     * @throws FitsException _more_
     */
    public abstract void setRow(int row, Object[] newRow)
     throws FitsException;

    /**
     * _more_
     *
     * @param col _more_
     * @param newCol _more_
     *
     * @throws FitsException _more_
     */
    public abstract void setColumn(int col, Object newCol)
     throws FitsException;

    /**
     * _more_
     *
     * @param row _more_
     * @param col _more_
     * @param element _more_
     *
     * @throws FitsException _more_
     */
    public abstract void setElement(int row, int col, Object element)
     throws FitsException;

    /**
     * _more_
     *
     * @param newRow _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public abstract int addRow(Object[] newRow) throws FitsException;

    /**
     * _more_
     *
     * @param newCol _more_
     *
     * @return _more_
     *
     * @throws FitsException _more_
     */
    public abstract int addColumn(Object newCol) throws FitsException;

    /**
     * _more_
     *
     * @param row _more_
     * @param len _more_
     *
     * @throws FitsException _more_
     */
    public abstract void deleteRows(int row, int len) throws FitsException;

    /**
     * _more_
     *
     * @param row _more_
     * @param len _more_
     *
     * @throws FitsException _more_
     */
    public abstract void deleteColumns(int row, int len) throws FitsException;

    /**
     * _more_
     *
     * @param oldNcol _more_
     * @param hdr _more_
     *
     * @throws FitsException _more_
     */
    public abstract void updateAfterDelete(int oldNcol, Header hdr)
     throws FitsException;

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract int getNCols();

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract int getNRows();

}
