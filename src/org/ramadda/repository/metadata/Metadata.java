/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.Role;
import org.ramadda.repository.database.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

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



@SuppressWarnings("unchecked")
public class Metadata implements Constants {
    public static final int MAX_LENGTH = 32000;

    //Not sure how big the extra field is
    public static final int MAX_LENGTH_EXTRA = 20000;
    public static final int INDEX_BASE = 1;
    public static String TAG_EXTRA = "extra";
    public static String TAG_ATTRIBUTES = "attributes";
    public static String TAG_ATTR = "attr";
    public static String ATTR_INDEX = "index";
    public static final String DFLT_EXTRA = "";
    public static final String DFLT_ATTR = "";
    public static final String DFLT_ID = "";
    public static final String DFLT_ENTRYID = "";
    public static final int PRIORITY_UNDEFINED = 0;

    private MetadataType metadataType;
    private String type;
    private Entry entry;
    private String id;
    private String entryId;
    private int priority = PRIORITY_UNDEFINED;
    private String attr1 = "";
    private String attr2 = "";
    private String attr3 = "";
    private String attr4 = "";
    private String extra;
    private Hashtable<Integer, String> extraMap;
    private boolean inherited = false;
    private String access="";
    private List<Role> accessList;
    private Object[] values;
    private boolean markedForDelete = false;

    public Metadata() {}
    
    public Metadata(Metadata that) {
        this("", "", that);
    }

    public Metadata(String id, String entryId, MetadataType type,  boolean inherited) {
        this(id, entryId, type, inherited, DFLT_ATTR, DFLT_ATTR, DFLT_ATTR, DFLT_ATTR, DFLT_EXTRA);
    }
    
    public Metadata(MetadataType type, String attr1, String attr2, String attr3,
                    String attr4, String extra) {
        this(DFLT_ID, DFLT_ENTRYID, type, false, attr1, attr2, attr3, attr4, extra);
    }
    
    public Metadata(MetadataType type) {
        this(DFLT_ID, DFLT_ENTRYID, type, false, DFLT_ATTR, DFLT_ATTR,
             DFLT_ATTR, DFLT_ATTR, DFLT_EXTRA);
    }


    
    public Metadata(String id, String entryId, MetadataType type,
                    boolean inherited, Object[] values) {
        this.id        = id;
        this.entryId   = id;
        this.inherited = inherited;
        this.values    = values;
	setMetadataType(type);
    }


    
    public Metadata(String id, String entryId, MetadataType type,
                    boolean inherited, String attr1, String attr2,
                    String attr3, String attr4, String extra) {
	//        this(id, entryId, type.getId(), inherited, attr1, attr2, attr3,
	//             attr4, extra);
        this.id        = id;
        this.entryId   = entryId;
	setMetadataType(type);
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

    /*
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
    */
    
    public Metadata(String id, String entryId, MetadataType type, String[] attrs) {
        this.id      = id;
        this.entryId = entryId;
	setMetadataType(type);
        for (int i = 0; i < attrs.length; i++) {
            this.setAttr(i + 1, attrs[i]);
        }
    }


    
    public Metadata(String id, String entryId, Metadata that) {
        this.id        = id;
        this.entryId   = entryId;
	setMetadataType(that.metadataType);
        this.inherited = that.inherited;
	this.access=that.access;
	this.accessList=that.accessList;	
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

    
    public static List<Metadata> sort(List<Metadata> metadata) {
        ArrayList<Metadata> sorted =
            (ArrayList<Metadata>) new ArrayList(metadata);
        Collections.sort(sorted, new MetadataCompare());

        return sorted;
    }


    
    private static class MetadataCompare implements Comparator<Metadata> {

        
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

    
    public static boolean lengthOK(String s) {
        if (s == null) {
            return true;
        }

        return s.length() < MAX_LENGTH;
    }



    
    public void setId(String value) {
        id = value;
    }

    
    public String getId() {
        return id;
    }


    
    public void setEntryId(String value) {
        entryId = value;
    }

    
    public String getEntryId() {
        return entryId;
    }
    
    public String getType() {
        return type;
    }

    public boolean isType(String t) {
        return type.equals(t);
    }    
    
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
	int index =Integer.valueOf(idx);
        if (value == null) {
            extraMap.remove(index);
        } else {
            extraMap.put(index, value);
        }
        this.extra = null;
    }


    
    public void setAttr1(String value) {
        attr1 = value;
    }




    
    public String getAttr1() {
        return getValue(1, attr1);
    }

    
    public void setAttr2(String value) {
        attr2 = value;
    }

    
    public String getAttr2() {
        return getValue(2, attr2);
    }

    
    public void setAttr3(String value) {
        attr3 = value;
    }

    
    public String getAttr3() {
        return getValue(3, attr3);
    }

    
    public void setAttr4(String value) {
        attr4 = value;
    }

    
    public String getAttr4() {
        return getValue(4, attr4);
    }


    
    public void setInherited(boolean value) {
        inherited = value;
    }

    
    public boolean getInherited() {
        return inherited;
    }


    public void setAccess (String value) {
	access = value;
	accessList=new ArrayList<Role>();
	if(Utils.stringDefined(value)) {
	    for(String r:Utils.split(value,",",true,true)) {
		accessList.add(new Role(r));
	    }
	}
    }

    public List<Role> getAccessList() {
	return accessList;
    }


    public String getAccess () {
	return access;
    }



    
    public String toString() {
        return "id:"+ id +" entry:" + entryId + " Type:" + metadataType+
	    " type:" + type + " attr1:" + attr1
	    + " attr2:" + attr2 + " attr3:" + attr3 + " attr4:" + attr4;
    }


    
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


    
    public String getAttr(int idx,boolean...checkExtra) {
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
	if(checkExtra.length>0 && !checkExtra[0]) return null;
        Hashtable<Integer, String> extraMap = getExtraMap();
        return extraMap.get(Integer.valueOf(idx));
        //        throw new IllegalArgumentException("Bad attr idx:" + idx);



    }




    
    public static class Type {
        public static final String DEFAULT_CATEGORY = "Properties";
        public static final int SEARCHABLE_ATTR1 = 1 << 0;
        public static final int SEARCHABLE_ATTR2 = 1 << 1;
        public static final int SEARCHABLE_ATTR3 = 1 << 3;
        public static final int SEARCHABLE_ATTR4 = 1 << 4;
        public int searchableMask = 0;
        private String type;
        private String category = DEFAULT_CATEGORY;
        private String label;
        private boolean isEnumerated = false;

        
        public Type(String type) {
            this.type = type;
            label     = type.replace("_", " ");
            label     = label.replace(".", " ");
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
        }

        
        public Type(String type, String label) {
            this(type, label, DEFAULT_CATEGORY, false);
        }

        
        public Type(String type, String label, String category) {
            this(type, label, category, false);
        }

        
        public Type(String type, String label, boolean enumerated) {
            this(type, label, DEFAULT_CATEGORY, enumerated);
        }

        
        public Type(String type, String label, String category,
                    boolean enumerated) {
            this.type         = type;
            this.label        = label;
            this.category     = category;
            this.isEnumerated = enumerated;
        }

        
        public void setSearchableMask(int mask) {
            searchableMask = mask;
        }


        
        public boolean isSearchable(int mask) {
            return (searchableMask & mask) != 0;
        }

        
        public boolean isAttr1Searchable() {
            return isSearchable(SEARCHABLE_ATTR1);
        }

        
        public boolean isAttr2Searchable() {
            return isSearchable(SEARCHABLE_ATTR2);
        }

        
        public boolean isAttr3Searchable() {
            return isSearchable(SEARCHABLE_ATTR3);
        }

        
        public boolean isAttr4Searchable() {
            return isSearchable(SEARCHABLE_ATTR4);
        }



        
        public boolean isType(String type) {
            return this.type.equals(type);
        }

        
        public boolean equals(Object o) {
            if ( !getClass().equals(o.getClass())) {
                return false;
            }
            Type that = (Type) o;

            return type.equals(that.type);
        }

        
        public String toString() {
            return type;
        }


        
        public void setType(String value) {
            type = value;
        }

        
        public String getType() {
            return type;
        }

        
        public void setLabel(String value) {
            label = value;
        }

        
        public String getLabel() {
            return label;
        }

        
        public void setIsEnumerated(boolean value) {
            isEnumerated = value;
        }

        
        public boolean getIsEnumerated() {
            return isEnumerated;
        }


        
        public void setCategory(String value) {
            category = value;
        }

        
        public String getCategory() {
            return category;
        }



    }

    
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


    
    public void setEntry(Entry value) {
        entry = value;
        if (value != null) {
            entryId = value.getId();
        }
    }

    
    public Entry getEntry() {
        return entry;
    }

    
    public void setExtra(String value) {
        this.extra = value;
    }

    
    public String getExtraRaw() {
	return extra;
    }

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


    public int getExtraCount() {
	if(!Utils.stringDefined(extra)) return 0;
	return getExtraMap().size();
    }


    public Hashtable<Integer, String> getExtraMap() {
        if (extraMap != null) {
            return extraMap;
        }
        Hashtable<Integer, String> tmp = extraToMap(extra);
        extraMap = tmp;

        //        extra    = null;
        return tmp;
    }



    
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

    
    public void setPriority(int p) {
        priority = p;
    }

    
    public static String trimToMaxLength(String s) {
        if (s == null) {
            return s;
        }
        if (s.length() > MAX_LENGTH) {}

        return s;
    }


    
    public void setMarkedForDelete(boolean value) {
        markedForDelete = value;
    }

    
    public boolean getMarkedForDelete() {
        return markedForDelete;
    }


    
    public void setMetadataType (MetadataType value) {
	metadataType = value;
	if(metadataType!=null)
	    this.type    = metadataType.getId();
	else
	    System.err.println("Bad type:" + this + " " + Utils.getStack(10));
	
    }

    
    public MetadataType getMetadataType () {
	return metadataType;
    }



}
