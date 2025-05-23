/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.type.GenericTypeHandler;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class GranuleTypeHandler extends GenericTypeHandler {

    String collectionId = null;

    public GranuleTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        /*
        if ( !newEntry) {
            return;
        }
        if ( !entry.isFile()) {
            return;
        }
        initializeGranuleEntry(entry);
        */
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        if ( !entry.isFile()) {
            return;
        }
        initializeGranuleEntry(entry);
    }

    public void initializeGranuleEntry(Entry entry) throws Exception {
        //        System.err.println("initializeGranuleEntry:" + entry.getName());
        collectionId = "";
        Entry parent = entry.getParentEntry();
        while (parent != null) {
            if (parent.getTypeHandler().isType(
                    getTypeProperty("collection_type", ""))) {
                collectionId = parent.getId();

                break;
            }
            parent = parent.getParentEntry();
        }
        if (collectionId.equals("")) {
            System.err.println("Could not find collection:" + entry);
        }

        Object[] values = getEntryValues(entry);
        //        System.err.println ("initializeGranuleEntry:" + getColumns());
        for (Column column : getColumns()) {
            //            System.err.println ("    name:" + column.getName());
            if (column.getName().equals("collection_id")) {
                values[column.getOffset()] = collectionId;
            }
        }
    }

    public void formatColumnHtmlValue(Request request, Entry entry,
                                      Column column, StringBuffer tmpSb,
                                      Object[] values)
            throws Exception {
        Entry collection = null;
        if (column.isEnumeration() && (values != null)
                && (values[0] != null)) {  // get enum values from Collection
            collection = getRepository().getEntryManager().getEntry(request,
                    (String) values[0]);
        }
        if (collection != null) {
            CollectionTypeHandler th =
                (CollectionTypeHandler) collection.getTypeHandler();
            LinkedHashMap enumMap = th.getColumnEnumTable(column);
            String        s = entry.getStringValue(request, column,"");
            String        label   = (String) enumMap.get(s);
            if (label != null) {
                s = label;
            }
            tmpSb.append(s);
        } else {
            column.formatValue(request, entry, tmpSb, Column.OUTPUT_HTML,
                               values, false);
        }
    }

    public static Entry getCollectionEntry(Request request, Entry granule) {
        if (granule == null) {
            return null;
        }
        if (granule.getTypeHandler() instanceof GranuleTypeHandler) {
	    String collectionEntryId = granule.getStringValue(request,0,null);
	    if (collectionEntryId != null) {
		try {
		    Entry collection =
			granule.getTypeHandler().getEntryManager()
			.getEntry(request, collectionEntryId);

		    return collection;
		} catch (Exception e) {
		    return null;
                }
            }
        }

        return null;
    }

}
