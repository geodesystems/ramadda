/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.database;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ColumnInfo {

    /** _more_ */
    public static final int TYPE_TIMESTAMP = 1;

    /** _more_ */
    public static final int TYPE_VARCHAR = 2;

    /** _more_ */
    public static final int TYPE_INTEGER = 3;

    /** _more_ */
    public static final int TYPE_DOUBLE = 4;

    /** _more_ */
    public static final int TYPE_CLOB = 5;

    /** _more_ */
    public static final int TYPE_BIGINT = 6;

    /** _more_ */
    public static final int TYPE_SMALLINT = 7;

    /** _more_ */
    public static final int TYPE_TINYINT = 8;

    /** _more_ */
    public static final int TYPE_TIME = 9;

    /** _more_ */
    public static final int TYPE_BLOB = 10;

    /** _more_ */
    public static final int TYPE_UNKNOWN = 11;

    /** _more_ */
    private String name;

    /** _more_ */
    private String typeName;

    /** _more_ */
    private int type;

    /** _more_ */
    private int size;

    /**
     * _more_
     */
    public ColumnInfo() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param typeName _more_
     * @param type _more_
     * @param size _more_
     */
    public ColumnInfo(String name, String typeName, int type, int size) {
        this.name     = name;
        this.typeName = typeName;
        this.type     = convertType(type);
        this.size     = size;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "Column: " + name + " " + type + " size:" + size + " ";
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public int convertType(int type) {
        //TODO: Not sure what the different types for time things should be
        if (type == java.sql.Types.TIMESTAMP) {
            return TYPE_TIMESTAMP;
        } else if (type == java.sql.Types.DATE) {
            return TYPE_TIMESTAMP;
        } else if (type == java.sql.Types.TIME) {
            return TYPE_TIME;
        } else if (type == java.sql.Types.VARCHAR) {
            return TYPE_VARCHAR;
            //        } else if (type == java.sql.Types.TEXT) {
            //            return TYPE_VARCHAR;
        } else if (type == java.sql.Types.INTEGER) {
            return TYPE_INTEGER;
        } else if (type == java.sql.Types.DOUBLE) {
            return TYPE_DOUBLE;
        } else if (type == java.sql.Types.CLOB) {
            return TYPE_CLOB;
        } else if (type == java.sql.Types.BLOB) {
            return TYPE_BLOB;
        } else if (type == java.sql.Types.BIGINT) {
            return TYPE_BIGINT;
        } else if (type == java.sql.Types.SMALLINT) {
            return TYPE_SMALLINT;
        } else if (type == java.sql.Types.TINYINT) {
            return TYPE_TINYINT;
        } else if (type == java.sql.Types.CHAR) {
            return TYPE_TINYINT;
        } else if (type == java.sql.Types.BIT) {
            return TYPE_TINYINT;
        } else if (type == java.sql.Types.DECIMAL) {
            return TYPE_DOUBLE;
        } else if (typeName.equalsIgnoreCase("text")) {
            return TYPE_CLOB;
        } else {
            return TYPE_UNKNOWN;
            //            throw new IllegalArgumentException("Unknown database type:"
            //                    + type + " " + typeName);
            //            throw new IllegalArgumentException("Unknown sqltype:" + type);
        }
    }


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return this.name;
    }

    /**
     *  Set the TypeName property.
     *
     *  @param value The new value for TypeName
     */
    public void setTypeName(String value) {
        this.typeName = value;
    }

    /**
     *  Get the TypeName property.
     *
     *  @return The TypeName
     */
    public String getTypeName() {
        return this.typeName;
    }

    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(int value) {
        this.type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public int getType() {
        return this.type;
    }

    /**
     *  Set the Size property.
     *
     *  @param value The new value for Size
     */
    public void setSize(int value) {
        this.size = value;
    }

    /**
     *  Get the Size property.
     *
     *  @return The Size
     */
    public int getSize() {
        return this.size;
    }



}
