/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

public interface DataTypes {

    public static final String DATATYPE_SYNTHETIC= "synthetic";
    public static final String DATATYPE_STRING = "string";
    public static final String DATATYPE_JSONLIST = "jsonlist";
    public static final String DATATYPE_ENTRY = "entry";
    public static final String DATATYPE_ENTRY_LIST = "entrylist";    
    public static final String DATATYPE_FILE = "file";
    public static final String DATATYPE_PASSWORD = "password";
    public static final String DATATYPE_API_KEY = "apikey";    
    public static final String DATATYPE_NOEDIT = "noedit";
    public static final String DATATYPE_CLOB = "clob";
    public static final String DATATYPE_EMAIL = "email";
    public static final String DATATYPE_KEY = "key";
    public static final String DATATYPE_INT = "int";
    public static final String DATATYPE_DOUBLE = "double";
    public static final String DATATYPE_PERCENTAGE = "percentage";
    public static final String DATATYPE_BOOLEAN = "boolean";
    public static final String DATATYPE_ENUMERATION = "enumeration";
    public static final String DATATYPE_ENUMERATIONPLUS = "enumerationplus";
    public static final String DATATYPE_MULTIENUMERATION = "multienumeration";
    public static final String DATATYPE_LIST = "list";
    public static final String DATATYPE_LATLONBBOX = "latlonbbox";
    public static final String DATATYPE_LATLON = "latlon";
    public static final String DATATYPE_SKIP = "skip";
    public static final String DATATYPE_WIKI = "wiki";
    public static final String DATATYPE_URL = "url";
    public static final String DATATYPE_GROUP = "group";
    public static final String DATATYPE_COLORTABLE = "colortable";
    public static final String DATATYPE_DATE = "date";
    public static final String DATATYPE_DATETIME = "datetime";
    public static final String DATATYPE_DEPENDENTENUMERATION =    "dependentenumeration";

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
	DATATYPE_LATLON,
	DATATYPE_ENTRY,
	DATATYPE_ENTRY_LIST
    };

}
