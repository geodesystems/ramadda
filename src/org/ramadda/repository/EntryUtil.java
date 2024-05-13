/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.database.*;


import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.IO;
import org.ramadda.util.SelectionRectangle;



import org.ramadda.util.TTLCache;
import org.ramadda.util.TTLObject;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;
import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.geom.Rectangle2D;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class does most of the work of managing repository content
 */
@SuppressWarnings("unchecked")
public class EntryUtil extends RepositoryManager {

    //Cache for 1 hour

    /** _more_ */
    private TTLObject<Hashtable<String, Integer>> typeCache =
        new TTLObject<Hashtable<String, Integer>>(60 * 60 * 1000,
                      "Entry Type Count Cache");



    /**
     * _more_
     *
     * @param repository _more_
     */
    private EntryUtil(Repository repository) {
        super(repository);
    }

    /**
     *
     * @param repository _more_
     *
     * @return _more_
     */
    public static EntryUtil newEntryUtil(Repository repository) {
        return new EntryUtil(repository);
    }



    /**
     * _more_
     */
    public synchronized void clearCache() {
        if (typeCache != null) {
            typeCache.clearCache();
        }
    }


    /**
     *
     * @param entries _more_
     *
     * @return _more_
     */
    public static List<Entry> getGroups(List<Entry> entries) {
        List<Entry> groups = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if (entry.isGroup()) {
                groups.add(entry);
            }
        }

        return groups;
    }

    /**
     *
     * @param entries _more_
     *
     * @return _more_
     */
    public static List<Entry> getNonGroups(List<Entry> entries) {
        List<Entry> nongroups = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if ( !entry.isGroup()) {
                nongroups.add(entry);
            }
        }

        return nongroups;
    }


    public static List<Entry> applySkip(List<Entry> entries, int skip) {
	if(skip>=1) {
	    List<Entry> skipped = new ArrayList<Entry>();
	    int cnt=-1;
	    for(int i=0;i<entries.size();i++) {
		if(cnt-->0) {
		    continue;
		}
		cnt = skip;
		skipped.add(entries.get(i));
	    }
	    entries = skipped;
	}
	return entries;
    }
	
    public static List<Entry> applySample(List<Entry> entries, double prob) {
	if(prob>0) {
	    List<Entry> skipped = new ArrayList<Entry>();
	    for(int i=0;i<entries.size();i++) {
		double r = Math.random();
		if(r <= prob)
		    skipped.add(entries.get(i));
	    }
	    entries = skipped;
	}
	return entries;
    }



    /*
      the metadata needs to be the sort order metadata
     */
    public static List<Entry> sortEntriesOnMetadata(List<Entry> entries,Metadata metadata) {
	String by = metadata.getAttr1();
	boolean descending=!metadata.getAttr2().equals("true");
	return 	sortEntriesOn(entries,by, descending);
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnName(List<Entry> entries,
            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1     = (Entry) o1;
                Entry e2     = (Entry) o2;
                int   result = e1.getName().compareToIgnoreCase(e2.getName());
                if (descending) {
                    if (result >= 1) {
                        return -1;
                    } else if (result <= -1) {
                        return 1;
                    }

                    return 0;
                }

                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }

    /**
     *
     * @param entries _more_
     * @param descending _more_
      * @return _more_
     */
    public static List<Entry> sortEntriesOnNumber(List<Entry> entries,
            final boolean descending) {
        List tmp = new ArrayList();
        for (Entry entry : entries) {
            String s1 = StringUtil.findPattern(entry.getName(), "([0-9]+)");
            if (s1 == null) {
                s1 = "9999";
            }
            double v1 = Double.parseDouble(s1);
            tmp.add(new Object[] { entry, v1 });
        }
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Object[] t1     = (Object[]) o1;
                Object[] t2     = (Object[]) o2;
                double   v1     = (double) t1[1];
                double   v2     = (double) t2[1];
                int      result = (v1 < v2)
                                  ? -1
                                  : (v1 == v2)
                                    ? 0
                                    : 1;
                if (descending) {
                    if (result >= 1) {
                        return -1;
                    } else if (result <= -1) {
                        return 1;
                    }

                    return 0;
                }

                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = tmp.toArray();
        Arrays.sort(array, comp);
        List<Entry> result = new ArrayList<Entry>();
        for (Object o : array) {
            Object[] tuple = (Object[]) o;
            result.add((Entry) tuple[0]);
        }

        return result;
    }


    /**
     *
     * @param entry _more_
     * @param entries _more_
     *
     * @return _more_
     */
    public static Entry getPrev(Entry entry, List<Entry> entries) {
        //      System.err.println("prev:" + entry+" list:" + entries);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e.getId() == entry.getId()) {
                //              System.err.println("\tgot:" + i +" " + entries.size());
                if (i > 0) {
                    //              System.err.println("\tok");
                    return entries.get(i - 1);
                }

                return null;
            }
        }

        return null;
    }

    /*
     */

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param root _more_
     * @param tree _more_
     * @param sort _more_
     * @param ascending _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getNext(Request request, Entry entry, Entry root,
                         boolean tree, String sort, boolean ascending)
            throws Exception {
        if (entry == null) {
            return null;
        }
        if (tree && entry.equalsEntry(root)) {
            List<Entry> first =
                sortEntriesOn(getEntryManager().getChildren(request, entry),
                              sort, !ascending);

            return (first.size() > 0)
                   ? first.get(0)
                   : null;
        }


        Entry parent = entry.getParentEntry();
        if (parent == null) {
            return null;
        }
        List<Entry> children =
            sortEntriesOn(getEntryManager().getChildren(request, parent),
                          sort, !ascending);
        Entry next = getNext(entry, children);
        if ((next != null) || !tree) {
            return next;
        }

        if (tree) {
            if ((root != null) && root.equalsEntry(parent)) {
                return null;
            }

            return getNext(request, parent, root, tree, sort, ascending);
        }

        return null;
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param root _more_
     * @param tree _more_
     * @param sort _more_
     * @param ascending _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getPrev(Request request, Entry entry, Entry root,
                         boolean tree, String sort, boolean ascending)
            throws Exception {
        if (entry == null) {
            return null;
        }
        if (tree && entry.equalsEntry(root)) {
            return null;
        }

        Entry parent = entry.getParentEntry();
        if (parent == null) {
            return null;
        }
        List<Entry> children =
            sortEntriesOn(getEntryManager().getChildren(request, parent),
                          sort, !ascending);
        Entry prev = getPrev(entry, children);
        if ((prev != null) || !tree) {
            return prev;
        }

        if (tree) {
            if ((root != null) && root.equalsEntry(parent)) {
                return null;
            }

            return getPrev(request, parent, root, tree, sort, ascending);
        }

        return null;
    }



    /**
     *
     * @param entry _more_
     * @param entries _more_
     *
     * @return _more_
     */
    public static Entry getNext(Entry entry, List<Entry> entries) {
        //      System.err.println("next:" + entry+" list:" + entries);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e.getId() == entry.getId()) {
                //              System.err.println("\tgot:" + i +" " + entries.size());
                if (i <= entries.size() - 2) {
                    //              System.err.println("\tallgood");
                    return entries.get(i + 1);
                }

                return null;
            }
        }

        return null;
    }

    /**
     *
     * @param list _more_
     * @param id _more_
     *
     * @return _more_
     */
    public static Entry findEntry(List<Entry> list, String id) {
        if (id == null) {
            return null;
        }
        for (Entry e : list) {
            if (e.getId().equals(id)) {
                return e;
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     * @param p _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnPattern(List<Entry> entries,
            final boolean descending, String p) {
        p = p.replaceAll("_LEFT_", "[").replaceAll("_RIGHT_", "]");
        final Pattern pattern = Pattern.compile(p);
        //      System.err.println("on pattern:" + pattern+":");
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry   e1 = (Entry) o1;
                Entry   e2 = (Entry) o2;
                Matcher m1 = pattern.matcher(e1.getName());
                Matcher m2 = pattern.matcher(e2.getName());
                if ( !m1.find() || !m2.find()) {
                    //                    System.err.println("No match: name1: " + e1.getName() + " name2: " + e2.getName());

                    return 0;
                }
                if (m1.groupCount() == 0) {
                    return 0;
                }
                if (m1.groupCount() != m2.groupCount()) {
                    System.err.println("bad match:");

                    return 0;
                }
                for (int i = 1; i <= m1.groupCount(); i++) {
                    String v1 = m1.group(i);
                    String v2 = m2.group(i);
                    if ((v1 == null) || (v2 == null)) {
                        return 0;
                    }
                    //              System.err.println("#" + i+" v1:" + v1  +" " + v2);
                    try {
                        double dv1    = Double.parseDouble(v1);
                        double dv2    = Double.parseDouble(v2);
                        int    result = (dv1 < dv2)
                                        ? -1
                                        : (dv1 == dv2)
                                          ? 0
                                          : 1;
                        if (descending) {
                            result = ((result >= 1)
                                      ? -1
                                      : (result < 0)
                                        ? 1
                                        : 0);
                        }
                        if (result != 0) {
                            return result;
                        }
                    } catch (Exception exc) {
                        System.err.println("Error parsing name:"
                                           + e1.getName() + " "
                                           + e2.getName() + " error:" + exc);

                        return 0;
                    }
                }

                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> doGroupAndNameSort(List<Entry> entries,
            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1     = (Entry) o1;
                Entry e2     = (Entry) o2;
                int   result = 0;
                if (e1.isGroup()) {
                    if (e2.isGroup()) {
                        result = e1.getFullName().compareTo(e2.getFullName());
                    } else {
                        result = -1;
                    }
                } else if (e2.isGroup()) {
                    result = 1;
                } else {
                    result = e1.getFullName().compareTo(e2.getFullName());
                }
                if (descending) {
                    return -result;
                }

                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnDate(List<Entry> entries,
            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                if (e1.getStartDate() < e2.getStartDate()) {
                    return (descending
                            ? 1
                            : -1);
                }
                if (e1.getStartDate() > e2.getStartDate()) {
                    return (descending
                            ? -1
                            : 1);
                }

                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnCreateDate(List<Entry> entries,
            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                if (e1.getCreateDate() < e2.getCreateDate()) {
                    return (descending
                            ? 1
                            : -1);
                }
                if (e1.getCreateDate() > e2.getCreateDate()) {
                    return (descending
                            ? -1
                            : 1);
                }

                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnEntryOrder(List<Entry> entries,
            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                if (e1.getEntryOrder() < e2.getEntryOrder()) {
                    return (descending
                            ? 1
                            : -1);
                }
                if (e1.getEntryOrder() > e2.getEntryOrder()) {
                    return (descending
                            ? -1
                            : 1);
                }

                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnChangeDate(List<Entry> entries,
            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                if (e1.getChangeDate() < e2.getChangeDate()) {
                    return (descending
                            ? 1
                            : -1);
                }
                if (e1.getChangeDate() > e2.getChangeDate()) {
                    return (descending
                            ? -1
                            : 1);
                }

                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }


    private static int compare(Date d1, Date d2) {
	if(d1==null && d2==null) return 0;
	if(d1==null) return 1;
	if(d2==null) return -1;	
	return compare(d1.getTime(),d2.getTime());
    }

    private static int compare(String d1, String d2) {
	if(d1==null && d2==null) return 0;
	if(d1==null) return 1;
	if(d2==null) return -1;	
	return d1.compareTo(d2);
    }    

    private static int compare(Integer d1, Integer d2) {
	if(d1==null && d2==null) return 0;
	if(d1==null) return 1;
	if(d2==null) return -1;	
	return d1.intValue()-d2.intValue();
    }

    private static int compare(Double d1, Double d2) {
	if(d1==null && d2==null) return 0;
	if(d1==null) return 1;
	if(d2==null) return -1;	
	return (int)(d1.doubleValue()-d2.doubleValue());
    }        

    /**
     * _more_
     *
     * @param l1 _more_
     * @param l2 _more_
     *
     * @return _more_
     */
    private static int compare(long l1, long l2) {
        if (l1 < l2) {
            return -1;
        } else if (l1 > l2) {
            return 1;
        }

        return 0;
    }

    /**
     * _more_
     *
     * @param e1 _more_
     * @param e2 _more_
     * @param on _more_
     *
     * @return _more_
     */
    private static int compareEntries(Entry e1, Entry e2, CompareOn on) {
	if(on.column!=null) {
	    if(e1.getTypeHandler().equals(e2.getTypeHandler())) {
		Object v1 = on.column.getObject(e1.getValues());
		Object v2 = on.column.getObject(e2.getValues());	    
		if(on.column.isDate()) {
		    return compare((Date)v1,(Date)v2);
		}
		if(on.column.isInteger()) {
		    return compare((Integer)v1,(Integer)v2);
		}		

		if(on.column.isDouble()) {
		    return compare((Double)v1,(Double)v2);
		}		
		if(on.column.isString() || on.column.isEnumeration()) {
		    return compare(v1==null?null:v1.toString(),
				   v2==null?null:v2.toString());
		}
		return 0;
	    }
	}
        if (on.is(ORDERBY_DATE) || on.is(ORDERBY_FROMDATE)) {
            return compare(e1.getStartDate(), e2.getStartDate());
        } else if (on.is(ORDERBY_TODATE)) {
            return compare(e1.getEndDate(), e2.getEndDate());
        } else if (on.is(ORDERBY_CHANGEDATE)) {
            return compare(e1.getChangeDate(), e2.getChangeDate());
        } else if (on.is(ORDERBY_CREATEDATE)) {
            return compare(e1.getCreateDate(), e2.getCreateDate());
        } else if (on.is(ORDERBY_NAME)) {
            return e1.getTypeHandler().getNameSort(e1).compareToIgnoreCase(e2.getTypeHandler().getNameSort(e2));
        } else if (on.is(ORDERBY_ENTRYORDER)) {
            return e1.getEntryOrder() - e2.getEntryOrder();
        } else if (on.is(ORDERBY_TYPE)) {
            return e1.getTypeHandler().getLabel().compareToIgnoreCase(
                e2.getTypeHandler().getLabel());
        } else if (on.is(ORDERBY_SIZE)) {
            return compare(e1.getResource().getFileSize(),
                           e2.getResource().getFileSize());
        } else if (on.is(ORDERBY_NUMBER)) {
	    File f1 =e1.getFile();
	    File f2 =e2.getFile();	    
	    if(f1!=null && f2!=null) {
		double v1 = IO.extractNumber(StorageManager.getOriginalFilename(f1.getName()),9999999);
		double v2 = IO.extractNumber(StorageManager.getOriginalFilename(f2.getName()),9999999);
		/*
		System.err.println("by number:" +
				   StorageManager.getOriginalFilename(f1.getName()) +" v:" + v1 + " " +
				   StorageManager.getOriginalFilename(f2.getName()) +" v:" +v2);
		*/
		return compare(v1,v2);
	    }
	    if(f1!=null) return -1;
	    if(f2!=null) return 1;
	    return 0;
	} else {
	    if(true) throw new IllegalStateException("SOrt order:" + on.on);
	    System.err.println("Unknown sort order:" + on.on);
	}

        return 0;
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param ons _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOn(List<Entry> entries, String ons,
                                            boolean descending) {
        return sortEntriesOn(entries, Utils.split(ons, ",", true, true),
                             descending);
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param ons _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOn(List<Entry> entries,
                                            final List<String> ons,
                                            final boolean descending) {
	if(ons.size()==1 && ons.get(0).equals("none")) return entries;
	return sortEntriesCompareOn(entries,makeCompareOn(ons,entries), descending);
    }


    private static List<CompareOn> makeCompareOn(List<String> ons,List<Entry> entries){
	List<CompareOn> compareOns = new ArrayList<CompareOn>();
	for(String on: ons) {
	    Column column=null;
	    if(on.startsWith("field:")) {
		on=on.substring("field:".length());
		if(entries.size()>0)
		    column = entries.get(0).getTypeHandler().findColumn(on);
	    }
	    compareOns.add(new CompareOn(on,column));
	}
	return compareOns;
    }

    private static List<Entry> sortEntriesCompareOn(List<Entry> entries,
						    final List<CompareOn> ons,
						    final boolean descending) {
	


        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                for (CompareOn on : ons) {
                    int result = compareEntries(e1, e2, on);
                    if (result != 0) {
                        if (descending) {
                            return -result;
                        }

                        return result;
                    }
                }

                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        entries = (List<Entry>) Misc.toList(array);

        return entries;
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param sorts _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntries(List<Entry> entries, String sorts,
                                          final boolean descending) {
        if (sorts.startsWith("number:")) {
            return sortEntriesOnPattern(entries, descending,
                                        sorts.substring(7));
        }

        return sortEntriesOn(entries, Utils.split(sorts, ",", true, true),
                             descending);
    }


    /**
     *
     * @param entry _more_
     * @param entries _more_
     *
     * @return _more_
     */
    public static int indexOf(Entry entry, List<Entry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            if (entry.getId().equals(entries.get(i).getId())) {
                return i;
            }
        }

        return -1;
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     * @param type _more_
     * @param sortOrderFieldIndex _more_
     *
     * @return _more_
     */
    public static List<Entry> sortEntriesOnField(List<Entry> entries,
            final boolean descending, final String type,
            final int sortOrderFieldIndex) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry   e1 = (Entry) o1;
                Entry   e2 = (Entry) o2;
                int     result;
                boolean isType1 = e1.getTypeHandler().isType(type);
                boolean isType2 = e2.getTypeHandler().isType(type);
                if (isType1 && isType2) {
                    Integer i1 =
                        (Integer) e1.getTypeHandler().getEntryValue(e1,
                            sortOrderFieldIndex);
                    Integer i2 =
                        (Integer) e2.getTypeHandler().getEntryValue(e2,
                            sortOrderFieldIndex);
                    if ((i1 < 0) && (i2 >= 0)) {
                        result = 1;
                    } else if ((i2 < 0) && (i1 >= 0)) {
                        result = -1;
                    } else {
                        result = i1.compareTo(i2);
                    }
                } else if (isType1) {
                    result = -1;
                } else if (isType2) {
                    result = 1;
                } else {
                    result = e1.getTypeHandler().getNameSort(e1).compareToIgnoreCase(e2.getTypeHandler().getNameSort(e2));
                }
                if (descending) {
                    if (result >= 1) {
                        return -1;
                    } else if (result <= -1) {
                        return 1;
                    }

                    return 0;
                }

                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);

        return (List<Entry>) Misc.toList(array);
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getTimezone(Request request, Entry entry) {
        try {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    new String[] { ContentMetadataHandler.TYPE_TIMEZONE },
                    true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                Metadata metadata = metadataList.get(0);

                return metadata.getAttr1();
            }
        } catch (Exception exc) {
            logError("getting timezone", exc);
	    exc.printStackTrace();
        }

        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Entry entry) {
        return getDateHandler().formatDate(request, entry.getStartDate(),
                                           getTimezone(request,entry));
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param type _more_
     *
     * @return _more_
     */
    public List<Entry> getEntriesWithType(List<Entry> entries, String type) {
        List<Entry> results = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if (entry.getTypeHandler().isType(type)) {
                results.add(entry);
            }
        }

        return results;
    }


    /**
     * _more_
     *
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int getEntryCount(TypeHandler typeHandler) throws Exception {
        Hashtable<String, Integer> typesWeHave = typeCache.get();
        if (typesWeHave == null) {
            typesWeHave = new Hashtable<String, Integer>();
            for (String type :
                    getRepository().getDatabaseManager().selectDistinct(
                        Tables.ENTRIES.NAME, Tables.ENTRIES.COL_TYPE, null)) {
                int cnt = getDatabaseManager().getCount(Tables.ENTRIES.NAME,
                              Clause.eq(Tables.ENTRIES.COL_TYPE, type));

                typesWeHave.put(type, Integer.valueOf(cnt));
            }
            typeCache.put(typesWeHave);
        }
        Integer cnt = typesWeHave.get(typeHandler.getType());
        if (cnt == null) {
            return 0;
        }

        return cnt.intValue();
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param type _more_
     *
     * @return _more_
     */
    public List<Entry> getEntriesOfType(List<Entry> entries, String type) {
        List<Entry> result = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if (entry.getTypeHandler().isType(type)) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param children _more_
     *
     * @return _more_
     */
    public Rectangle2D.Double getBounds(List<Entry> children) {
        Rectangle2D.Double rect = null;

        for (Entry child : children) {
	    rect = child.getTypeHandler().getBounds(child,rect);
        }

        return rect;
    }



    /**
     *
     * @param bbox _more_
     *
     * @return _more_
     */
    public static List<SelectionRectangle> getSelectionRectangles(
            SelectionRectangle bbox) {
        bbox.normalizeLongitude();
        List<SelectionRectangle> rectangles =
            new ArrayList<SelectionRectangle>();

        /*
   160                 20
    +------------------+
 ---------+---------+---------+------------
       180/-180     0      180/-180
        */

        //Check for a search crossing the dateline
        if (bbox.crossesDateLine()) {
            rectangles.add(new SelectionRectangle(bbox.getNorth(),
                    bbox.getWest(), bbox.getSouth(), 180));
            rectangles.add(new SelectionRectangle(bbox.getNorth(), -180,
                    bbox.getSouth(), bbox.getEast()));
        } else {
            rectangles.add(bbox);
        }

        return rectangles;

    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {}


    public static class Excluder {
	List<String> patterns;
	long sizeLimit;
	HashSet<String> excludedEntries = new HashSet<String>();

	public  Excluder(List<String> patterns, long sizeLimit) {
	    this.patterns = patterns;
	    this.sizeLimit = sizeLimit;
	}

	public boolean isEntryOk(Entry entry) {
	    if(!isEntryOkInner(entry)) {
		excludedEntries.add(entry.getId());
		return false;
	    }
	    return true;
	}

	private boolean isEntryOkInner(Entry entry) {	    
	    if(excludedEntries.size()>0) {
		Entry parent = entry.getParentEntry();
		while(parent!=null) {
		    if(excludedEntries.contains(parent.getId()))  {
			//			System.err.println("Parent excluded:" + entry);
			return false;
		    }
		    parent = parent.getParentEntry();
		}
	    }


	    if(sizeLimit>=0 && entry.isFile()) {
		if(entry.getResource().getFileSize()>sizeLimit) {
		    //		    System.err.println("Size Exclude:" + entry +" size:" + entry.getResource().getFileSize());
		    return false;
		}
	    }


	    if(patterns!=null) {
		for(String exclude: patterns) {
		    String name = entry.getName();
		    if(name!=null && (name.matches(exclude) || name.indexOf(exclude)>=0)) {
			//			System.err.println("Pattern Exclude:" + name);
			return false;
		    }
		}
	    }

	    return true;
	}
    }
	

    public static class CompareOn {
	String on;
	Column column;
	public CompareOn(String on,Column column) {
	    this.on = on;
	    this.column = column;
	}
	public boolean is(String on) {
	    return this.on.equals(on);
	}
    }


}
