/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public interface DataTypes {

    public static final String DATATYPE_SYNTHETIC= "synthetic";

    /** _more_ */
    public static final String DATATYPE_STRING = "string";

    /** _more_ */
    public static final String DATATYPE_JSONLIST = "jsonlist";

    /** _more_ */
    public static final String DATATYPE_ENTRY = "entry";
    public static final String DATATYPE_ENTRY_LIST = "entrylist";    

    /** _more_ */
    public static final String DATATYPE_FILE = "file";

    /** _more_ */
    public static final String DATATYPE_PASSWORD = "password";
    public static final String DATATYPE_API_KEY = "apikey";    

    /** _more_ */
    public static final String DATATYPE_CLOB = "clob";

    /** _more_ */
    public static final String DATATYPE_EMAIL = "email";

    /** _more_ */
    public static final String DATATYPE_KEY = "key";

    /** _more_ */
    public static final String DATATYPE_INT = "int";

    /** _more_ */
    public static final String DATATYPE_DOUBLE = "double";

    /** _more_ */
    public static final String DATATYPE_PERCENTAGE = "percentage";

    /** _more_ */
    public static final String DATATYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String DATATYPE_ENUMERATION = "enumeration";

    /** _more_ */
    public static final String DATATYPE_ENUMERATIONPLUS = "enumerationplus";

    public static final String DATATYPE_MULTIENUMERATION = "multienumeration";

    /** _more_ */
    public static final String DATATYPE_LIST = "list";

    /** _more_ */
    public static final String DATATYPE_LATLONBBOX = "latlonbbox";

    /** _more_ */
    public static final String DATATYPE_LATLON = "latlon";


    /** _more_ */
    public static final String DATATYPE_SKIP = "skip";

    /** _more_ */
    public static final String DATATYPE_WIKI = "wiki";

    /** _more_ */
    public static final String DATATYPE_URL = "url";

    /** _more_ */
    public static final String DATATYPE_GROUP = "group";

    /** _more_ */
    public static final String DATATYPE_COLORTABLE = "colortable";


    /** _more_ */
    public static final String DATATYPE_DATE = "date";

    /** _more_ */
    public static final String DATATYPE_DATETIME = "datetime";

    /** _more_ */
    public static final String DATATYPE_DEPENDENTENUMERATION =
        "dependentenumeration";

    public static final String[] BASE_TYPES = {
	DATATYPE_STRING,
	DATATYPE_ENUMERATION,
	DATATYPE_ENUMERATIONPLUS,
	DATATYPE_LIST,
	DATATYPE_INT,
	DATATYPE_DOUBLE,
	DATATYPE_DATE,
	DATATYPE_DATETIME,
	DATATYPE_URL,
	DATATYPE_WIKI,
	DATATYPE_LATLON
    };

}
