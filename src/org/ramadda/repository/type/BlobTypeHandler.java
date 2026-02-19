/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.util.Hashtable;

@SuppressWarnings("unchecked")
public abstract class BlobTypeHandler extends GenericTypeHandler {

    public BlobTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public BlobTypeHandler(Repository repository, String type,
                           String description) {
        super(repository, type, description);
    }

    public  String getValuesColumn() {
	return "undefined";
    }

    public void putEntryProperty(Request request, Entry entry, String key, Object value)
            throws Exception {
        Hashtable props = getProperties(request,entry);
        props.put(key, value);
        setProperties(entry, props);
    }

    public Hashtable getProperties(Request request, Entry entry) throws Exception {
        if (entry == null) {
            return new Hashtable();
        }
        Hashtable properties = null;
	String value = entry.getStringValue(request, getValuesColumn(),null);
	if (stringDefined(value)) {
	    properties = (Hashtable) Repository.decodeObject(value);
	}
	if (properties == null) {
	    properties = new Hashtable();
        }

        return properties;
    }



    protected void setProperties(Entry entry, Hashtable properties)
            throws Exception {
	String  encoded = Repository.encodeObject(properties);
	entry.setValue(getValuesColumn(), encoded);
    }

}
