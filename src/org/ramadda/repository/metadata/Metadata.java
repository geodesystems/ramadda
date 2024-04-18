/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;


import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;


import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class Metadata implements Constants {

    /** _more_ */
    public static final int MAX_LENGTH = 32000;

    //Not sure how big the extra field is

    /** _more_ */
    public static final int MAX_LENGTH_EXTRA = 20000;

    /** _more_ */
    public static final int INDEX_BASE = 1;

    /** _more_ */
    public static String TAG_EXTRA = "extra";

    /** _more_ */
    public static String TAG_ATTRIBUTES = "attributes";

    /** _more_ */
    public static String TAG_ATTR = "attr";

    /** _more_ */
    public static String ATTR_INDEX = "index";

    /** _more_ */
    public static final String DFLT_EXTRA = "";

    /** _more_ */
    public static final String DFLT_ATTR = "";

    /** _more_ */
    public static final String DFLT_ID = "";

    /** _more_ */
    public static final String DFLT_ENTRYID = "";

    /** _more_ */
    public static final int PRIORITY_UNDEFINED = 0;

    private MetadataType metadataType;

    /** _more_ */
    private Entry entry;

    /** _more_ */
    private String id;

    /** _more_ */
    private String entryId;

    /** _more_ */
    private String type;

    /** _more_ */
    private int priority = PRIORITY_UNDEFINED;

    /** _more_ */
    private String attr1 = "";

    /** _more_ */
    private String attr2 = "";

    /** _more_ */
    private String attr3 = "";

    /** _more_ */
    private String attr4 = "";

    /** _more_ */
    private String extra;

    /** _more_ */
    private Hashtable<Integer, String> extraMap;

    /** _more_ */
    private boolean inherited = false;

    /** _more_ */
    private Object[] values;


    /**  */
    private boolean markedForDelete = false;

    /**
     * _more_
     */
    public Metadata() {}


    /**
     * _more_
     *
     * @param that _more_
     */
    public Metadata(Metadata that) {
        this("", "", that);
    }


    /**
     * _more_
     *
     * @param type _more_
     */
    public Metadata(String type) {
        this(DFLT_ID, DFLT_ENTRYID, type, false, DFLT_ATTR, DFLT_ATTR,
             DFLT_ATTR, DFLT_ATTR, DFLT_EXTRA);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     */
    public Metadata(String id, String entryId, String type,
                    boolean inherited) {
        this(id, entryId, type, inherited, DFLT_ATTR, DFLT_ATTR, DFLT_ATTR,
             DFLT_ATTR, DFLT_EXTRA);
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     * @param extra _more_
     */
    public Metadata(String type, String attr1, String attr2, String attr3,
                    String attr4, String extra) {
        this(DFLT_ID, DFLT_ENTRYID, type, false, attr1, attr2, attr3, attr4,
             extra);
    }


    /**
     * _more_
     *
     * @param type _more_
     */
    public Metadata(MetadataType type) {
        this(DFLT_ID, DFLT_ENTRYID, type, false, DFLT_ATTR, DFLT_ATTR,
             DFLT_ATTR, DFLT_ATTR, DFLT_EXTRA);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     * @param values _more_
     */
    public Metadata(String id, String entryId, MetadataType type,
                    boolean inherited, Object[] values) {
        this.id        = id;
        this.entryId   = id;
        this.type      = type.getId();
        this.inherited = inherited;
        this.values    = values;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     * @param extra _more_
     */
    public Metadata(String id, String entryId, MetadataType type,
                    boolean inherited, String attr1, String attr2,
                    String attr3, String attr4, String extra) {
        this(id, entryId, type.getId(), inherited, attr1, attr2, attr3,
             attr4, extra);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     * @param extra _more_
     */
    public Metadata(String id, String entryId, String type,
                    boolean inherited, String attr1, String attr2,
                    String attr3, String attr4, String extra) {
        this.id        = id;
        this.entryId   = entryId;
        this.type      = type;
        this.inherited = inherited;
        setAttr1((attr1 != null)
                 ? attr1
                 : "");
        setAttr2((attr2 != null)
                 ? attr2
                 : "");
        setAttr3((attr3 != null)
                 ? attr3
                 : "");
        setAttr4((attr4 != null)
                 ? attr4
                 : "");
        this.extra = extra;
        if (this.extra == null) {
            this.extra = "";
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param attrs _more_
     */
    public Metadata(String id, String entryId, String type, String[] attrs) {
        this.id      = id;
        this.entryId = entryId;
        this.type    = type;
        for (int i = 0; i < attrs.length; i++) {
            this.setAttr(i + 1, attrs[i]);
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param that _more_
     */
    public Metadata(String id, String entryId, Metadata that) {
        this.id        = id;
        this.entryId   = entryId;
        this.type      = that.type;
        this.inherited = that.inherited;
        this.priority  = that.priority;
        this.attr1     = that.attr1;
        this.attr2     = that.attr2;
        this.attr3     = that.attr3;
        this.attr4     = that.attr4;
        this.extra     = that.getExtra();
        if (this.extra == null) {
            this.extra = "";
        }
        this.values = that.values;
    }


    public void sanitize() {
	id = HtmlUtils.strictSanitizeString(id);
	type = HtmlUtils.strictSanitizeString(type);
	attr1 = HtmlUtils.strictSanitizeString(attr1);
	attr2 = HtmlUtils.strictSanitizeString(attr2);
	attr3 = HtmlUtils.strictSanitizeString(attr3);
	attr4 = HtmlUtils.strictSanitizeString(attr4);
	extra = HtmlUtils.strictSanitizeString(extra);	
	extraMap=null;
    }

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public static List<Metadata> sort(List<Metadata> metadata) {
        ArrayList<Metadata> sorted =
            (ArrayList<Metadata>) new ArrayList(metadata);
        Collections.sort(sorted, new MetadataCompare());

        return sorted;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Dec 4, '13
     * @author         Enter your name here...
     */
    private static class MetadataCompare implements Comparator<Metadata> {

        /**
         * compare based on priority then type then attribute  values
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
        public int compare(Metadata o1, Metadata o2) {

            if ((o1.priority != PRIORITY_UNDEFINED)
		|| (o2.priority != PRIORITY_UNDEFINED)) {
                if (o1.priority > o2.priority) {
                    return -1;
                }
                if (o2.priority > o1.priority) {
                    return 1;
                }
            }


            int result = o1.getType().compareTo(o2.getType());
            if (result != 0) {
                return result;
            }
            result = o1.attr1.compareTo(o2.attr1);
            if (result != 0) {
                return result;
            }

            if ((o1.attr2 != null) && (o2.attr2 != null)) {
                result = o1.attr2.compareTo(o2.attr2);
            }

            return result;
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static boolean lengthOK(String s) {
        if (s == null) {
            return true;
        }

        return s.length() < MAX_LENGTH;
    }



    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }


    /**
     * Set the EntryId property.
     *
     * @param value The new value for EntryId
     */
    public void setEntryId(String value) {
        entryId = value;
    }

    /**
     * Get the EntryId property.
     *
     * @return The EntryId
     */
    public String getEntryId() {
        return entryId;
    }




    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

    public boolean isType(String t) {
        return type.equals(t);
    }    





    /**
     * _more_
     *
     * @param idx _more_
     * @param value _more_
     */
    public void setAttr(int idx, String value) {
        if (idx == 1) {
            attr1 = value;
        }
        if (idx == 2) {
            attr2 = value;
        }
        if (idx == 3) {
            attr3 = value;
        }
        if (idx == 4) {
            attr4 = value;
        }
        Hashtable<Integer, String> extraMap = getExtraMap();
        if (value == null) {
            extraMap.remove(Integer.valueOf(idx));
        } else {
            extraMap.put(Integer.valueOf(idx), value);
        }
        this.extra = null;
    }


    /**
     * Set the Attr1 property.
     *
     * @param value The new value for Attr1
     */
    public void setAttr1(String value) {
        attr1 = value;
    }




    /**
     * Get the Attr1 property.
     *
     * @return The Attr1
     */
    public String getAttr1() {
        return getValue(1, attr1);
    }

    /**
     * Set the Attr2 property.
     *
     * @param value The new value for Attr2
     */
    public void setAttr2(String value) {
        attr2 = value;
    }

    /**
     * Get the Attr2 property.
     *
     * @return The Attr2
     */
    public String getAttr2() {
        return getValue(2, attr2);
    }

    /**
     * Set the Attr3 property.
     *
     * @param value The new value for Attr3
     */
    public void setAttr3(String value) {
        attr3 = value;
    }

    /**
     * Get the Attr3 property.
     *
     * @return The Attr3
     */
    public String getAttr3() {
        return getValue(3, attr3);
    }

    /**
     * Set the Attr4 property.
     *
     * @param value The new value for Attr4
     */
    public void setAttr4(String value) {
        attr4 = value;
    }

    /**
     * Get the Attr4 property.
     *
     * @return The Attr4
     */
    public String getAttr4() {
        return getValue(4, attr4);
    }


    /**
     *  Set the Inherited property.
     *
     *  @param value The new value for Inherited
     */
    public void setInherited(boolean value) {
        inherited = value;
    }

    /**
     *  Get the Inherited property.
     *
     *  @return The Inherited
     */
    public boolean getInherited() {
        return inherited;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "id:"+ id +" entry:" + entryId + " type:" + type + " attr1:" + attr1
	    + " attr2:" + attr2 + " attr3:" + attr3 + " attr4:" + attr4;
    }


    /**
     * _more_
     *
     * @param index _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getValue(int index, String dflt) {
        if (values == null) {
            return dflt;
        }
        index--;
        if ((index > 0) && (index < values.length)
	    && (values[index] != null)) {
            return values[index].toString();
        }

        return null;
    }


    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    private Object getValue(int index) {
        if (values != null) {
            index--;
            if ((index > 0) && (index < values.length)) {
                return values[index];
            }

            return null;
        }

        return getAttr(index);
    }


    /**
     * _more_
     *
     * @param idx _more_
     *
     * @return _more_
     */
    public String getAttr(int idx) {
        if (values != null) {
            return (String) getValue(idx);
        }
        if (idx == 1) {
            return attr1;
        }
        if (idx == 2) {
            return attr2;
        }
        if (idx == 3) {
            return attr3;
        }
        if (idx == 4) {
            return attr4;
        }
        Hashtable<Integer, String> extraMap = getExtraMap();

        return extraMap.get(Integer.valueOf(idx));
        //        throw new IllegalArgumentException("Bad attr idx:" + idx);



    }




    /**
     * Class Type _more_
     *
     *
     * @author RAMADDA Development Team
     * @version $Revision: 1.3 $
     */
    public static class Type {

        /** _more_ */
        public static final String DEFAULT_CATEGORY = "Properties";

        /** _more_ */
        public static final int SEARCHABLE_ATTR1 = 1 << 0;

        /** _more_ */
        public static final int SEARCHABLE_ATTR2 = 1 << 1;

        /** _more_ */
        public static final int SEARCHABLE_ATTR3 = 1 << 3;

        /** _more_ */
        public static final int SEARCHABLE_ATTR4 = 1 << 4;

        /** _more_ */
        public int searchableMask = 0;

        /** _more_ */
        private String type;

        /** _more_ */
        private String category = DEFAULT_CATEGORY;


        /** _more_ */
        private String label;

        /** _more_ */
        private boolean isEnumerated = false;

        /**
         * _more_
         *
         * @param type _more_
         */
        public Type(String type) {
            this.type = type;
            label     = type.replace("_", " ");
            label     = label.replace(".", " ");
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         */
        public Type(String type, String label) {
            this(type, label, DEFAULT_CATEGORY, false);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         * @param category _more_
         */
        public Type(String type, String label, String category) {
            this(type, label, category, false);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         * @param enumerated _more_
         */
        public Type(String type, String label, boolean enumerated) {
            this(type, label, DEFAULT_CATEGORY, enumerated);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         * @param category _more_
         * @param enumerated _more_
         */
        public Type(String type, String label, String category,
                    boolean enumerated) {
            this.type         = type;
            this.label        = label;
            this.category     = category;
            this.isEnumerated = enumerated;
        }

        /**
         * _more_
         *
         * @param mask _more_
         */
        public void setSearchableMask(int mask) {
            searchableMask = mask;
        }


        /**
         * _more_
         *
         * @param mask _more_
         *
         * @return _more_
         */
        public boolean isSearchable(int mask) {
            return (searchableMask & mask) != 0;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr1Searchable() {
            return isSearchable(SEARCHABLE_ATTR1);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr2Searchable() {
            return isSearchable(SEARCHABLE_ATTR2);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr3Searchable() {
            return isSearchable(SEARCHABLE_ATTR3);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr4Searchable() {
            return isSearchable(SEARCHABLE_ATTR4);
        }



        /**
         * _more_
         *
         * @param type _more_
         *
         * @return _more_
         */
        public boolean isType(String type) {
            return this.type.equals(type);
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        public boolean equals(Object o) {
            if ( !getClass().equals(o.getClass())) {
                return false;
            }
            Type that = (Type) o;

            return type.equals(that.type);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return type;
        }


        /**
         * Set the Type property.
         *
         * @param value The new value for Type
         */
        public void setType(String value) {
            type = value;
        }

        /**
         * Get the Type property.
         *
         * @return The Type
         */
        public String getType() {
            return type;
        }

        /**
         * Set the Label property.
         *
         * @param value The new value for Label
         */
        public void setLabel(String value) {
            label = value;
        }

        /**
         * Get the Label property.
         *
         * @return The Label
         */
        public String getLabel() {
            return label;
        }

        /**
         *  Set the IsEnumerated property.
         *
         *  @param value The new value for IsEnumerated
         */
        public void setIsEnumerated(boolean value) {
            isEnumerated = value;
        }

        /**
         *  Get the IsEnumerated property.
         *
         *  @return The IsEnumerated
         */
        public boolean getIsEnumerated() {
            return isEnumerated;
        }


        /**
         * Set the Category property.
         *
         * @param value The new value for Category
         */
        public void setCategory(String value) {
            category = value;
        }

        /**
         * Get the Category property.
         *
         * @return The Category
         */
        public String getCategory() {
            return category;
        }



    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !getClass().equals(o.getClass())) {
            return false;
        }
        Metadata that = (Metadata) o;
        /*
	//        System.err.println(Misc.equals(this.type,  that.type) + " " +
	Misc.equals(this.attr1, that.attr1) + " " +
	Misc.equals(this.attr2, that.attr2) + " " +
	Misc.equals(this.attr3, that.attr3) + " " +
	Misc.equals(this.attr4, that.attr4) + " " +
	Misc.equals(this.entryId, that.entryId));*/

        return Misc.equals(this.type, that.type)
	    && Misc.equals(this.attr1, that.attr1)
	    && Misc.equals(this.attr2, that.attr2)
	    && Misc.equals(this.attr3, that.attr3)
	    && Misc.equals(this.attr4, that.attr4)
	    && Misc.equals(this.extra, that.extra)
	    && Misc.equals(this.entryId, that.entryId);
    }


    /**
     * Set the Entry property.
     *
     * @param value The new value for Entry
     */
    public void setEntry(Entry value) {
        entry = value;
        if (value != null) {
            entryId = value.getId();
        }
    }

    /**
     * Get the Entry property.
     *
     * @return The Entry
     */
    public Entry getEntry() {
        return entry;
    }

    /**
     *  Set the Extra property.
     *
     *  @param value The new value for Extra
     */
    public void setExtra(String value) {
        this.extra = value;
    }

    /**
     *  Get the Extra property.
     *
     *  @return The Extra
     */
    public String getExtra() {
        String tmp = this.extra;
        if (tmp == null) {
            if (extraMap == null) {
                return null;
            }
            tmp        = mapToExtra(extraMap);
            this.extra = tmp;
        }

        return tmp;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable<Integer, String> getExtraMap() {
        if (extraMap != null) {
            return extraMap;
        }
        Hashtable<Integer, String> tmp = extraToMap(extra);
        extraMap = tmp;

        //        extra    = null;
        return tmp;
    }



    /**
     * _more_
     *
     * @param extra _more_
     *
     * @return _more_
     */
    public static Hashtable<Integer, String> extraToMap(String extra) {
        Hashtable<Integer, String> tmp = new Hashtable<Integer, String>();
        if ((extra != null) && (extra.length() > 0)) {
            try {
                //System.err.println("** extra:" + extra + ":");
                //                System.err.println("");
                Element root = XmlUtil.getRoot(extra);
                if (root != null) {
                    List elements = XmlUtil.findChildren(root, TAG_EXTRA);

                    for (int j = 0; j < elements.size(); j++) {
                        Element extraNode = (Element) elements.get(j);
                        int index = XmlUtil.getAttribute(extraNode,
							 ATTR_INDEX, -1);
                        String text = XmlUtil.getChildText(extraNode);
                        if (text == null) {
                            text = "";
                        }
                        tmp.put(Integer.valueOf(index), text);
                    }
                }
            } catch (Exception exc) {
                System.err.println("Could not parse extra metadata:" + extra);
                //throw new RuntimeException(exc);
            }
        }

        return tmp;
    }



    /**
     * _more_
     *
     * @param map _more_
     *
     * @return _more_
     */
    public static String mapToExtra(Hashtable<Integer, String> map) {
        try {
            Document doc = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ATTRIBUTES,
                                          (Element) null);
            for (Enumeration keys = map.keys(); keys.hasMoreElements(); ) {
                Integer index = (Integer) keys.nextElement();
                String  extra = map.get(index);
                XmlUtil.create(doc, TAG_EXTRA, root, extra,
                               new String[] { ATTR_INDEX,
					      index.toString() });
            }

            return XmlUtil.toString(root, false);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param p _more_
     */
    public void setPriority(int p) {
        priority = p;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String trimToMaxLength(String s) {
        if (s == null) {
            return s;
        }
        if (s.length() > MAX_LENGTH) {}

        return s;
    }


    /**
     *  Set the MarkedForDelete property.
     *
     *  @param value The new value for MarkedForDelete
     */
    public void setMarkedForDelete(boolean value) {
        markedForDelete = value;
    }

    /**
     *  Get the MarkedForDelete property.
     *
     *  @return The MarkedForDelete
     */
    public boolean getMarkedForDelete() {
        return markedForDelete;
    }


    /**
       Set the MetadataType property.

       @param value The new value for MetadataType
    **/
    public void setMetadataType (MetadataType value) {
	metadataType = value;
    }

    /**
       Get the MetadataType property.

       @return The MetadataType
    **/
    public MetadataType getMetadataType () {
	return metadataType;
    }



}
