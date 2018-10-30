/*
* Copyright (c) 2008-2018 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository.type;


import org.ramadda.repository.*;




import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;


import java.util.Hashtable;





/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class BlobTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BlobTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     */
    public BlobTypeHandler(Repository repository, String type,
                           String description) {
        super(repository, type, description);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param key _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public void putEntryProperty(Entry entry, String key, Object value)
            throws Exception {
        Hashtable props = getProperties(entry);
        props.put(key, value);
        setProperties(entry, props);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Hashtable getProperties(Entry entry) throws Exception {
        if (entry == null) {
            return new Hashtable();
        }
        Hashtable properties = null;
        if (properties == null) {
            Object[] values = getEntryValues(entry);
            if ((values != null) && (values.length > 0)
                    && (values[getValuesIndex()] != null)) {
                properties = (Hashtable) Repository.decodeObject(
                    (String) values[getValuesIndex()]);
            }
            if (properties == null) {
                properties = new Hashtable();
            }
        }

        return properties;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getValuesIndex() {
        return 0;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param properties _more_
     *
     * @throws Exception _more_
     */
    protected void setProperties(Entry entry, Hashtable properties)
            throws Exception {
        Object[] values = getEntryValues(entry);
        values[getValuesIndex()] = Repository.encodeObject(properties);
    }



}
