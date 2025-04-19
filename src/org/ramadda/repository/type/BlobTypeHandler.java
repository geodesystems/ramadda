/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.util.Hashtable;

@SuppressWarnings("unchecked")
public class BlobTypeHandler extends GenericTypeHandler {

    public BlobTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public BlobTypeHandler(Repository repository, String type,
                           String description) {
        super(repository, type, description);
    }

    public void putEntryProperty(Entry entry, String key, Object value)
            throws Exception {
        Hashtable props = getProperties(entry);
        props.put(key, value);
        setProperties(entry, props);
    }

    public Hashtable getProperties(Entry entry) throws Exception {
        if (entry == null) {
            return new Hashtable();
        }
        Hashtable properties = null;
        if (properties == null) {
            Object[] values = getEntryValues(entry);
            int      index  = getValuesIndex();
            if ((values != null) && (index >= 0) && (index < values.length)
                    && (values[index] != null)) {
                properties = (Hashtable) Repository.decodeObject(
                    (String) values[index]);
            }
            if (properties == null) {
                properties = new Hashtable();
            }
        }

        return properties;
    }

    public int getValuesIndex() {
        return 0;
    }

    protected void setProperties(Entry entry, Hashtable properties)
            throws Exception {
        Object[] values = getEntryValues(entry);
        values[getValuesIndex()] = Repository.encodeObject(properties);
    }

}
