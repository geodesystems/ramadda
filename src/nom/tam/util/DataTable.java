
package nom.tam.util;


/* Copyright: Thomas McGlynn 1997-1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */

/**
 * This interface defines the properties that
 * a generic table should have.
 */
public interface DataTable {

    /**
     * _more_
     *
     * @param row _more_
     * @param newRow _more_
     *
     * @throws TableException _more_
     */
    public abstract void setRow(int row, Object newRow) throws TableException;

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public abstract Object getRow(int row);

    /**
     * _more_
     *
     * @param column _more_
     * @param newColumn _more_
     *
     * @throws TableException _more_
     */
    public abstract void setColumn(int column, Object newColumn)
     throws TableException;

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public abstract Object getColumn(int column);

    /**
     * _more_
     *
     * @param row _more_
     * @param col _more_
     * @param newElement _more_
     *
     * @throws TableException _more_
     */
    public abstract void setElement(int row, int col, Object newElement)
     throws TableException;

    /**
     * _more_
     *
     * @param row _more_
     * @param col _more_
     *
     * @return _more_
     */
    public abstract Object getElement(int row, int col);

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract int getNRows();

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract int getNCols();
}
