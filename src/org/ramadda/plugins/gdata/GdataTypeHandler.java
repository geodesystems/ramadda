/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.plugins.gdata;


import com.google.gdata.client.GoogleService;

import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.Category;

import com.google.gdata.data.Person;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;


import org.w3c.dom.*;

import java.io.File;

import java.net.URL;

import java.util.Hashtable;
import java.util.List;






import java.util.Set;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GdataTypeHandler extends GdataBaseTypeHandler {

    /** _more_ */
    private Hashtable<String, GoogleService> serviceMap =
        new Hashtable<String, GoogleService>();

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GdataTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    protected GoogleService getService(Entry entry) throws Exception {
        String userId   = getUserId(entry);
        String password = getPassword(entry);
        if ((userId == null) || (password == null)) {
            return null;
        }
        GoogleService service = serviceMap.get(userId);
        if (service != null) {
            return service;
        }
        service = doMakeService(userId, password);
        if (service == null) {
            return null;
        }
        serviceMap.put(userId, service);

        return service;
    }


    /**
     * _more_
     *
     * @param userId _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected GoogleService doMakeService(String userId, String password)
            throws Exception {
        return null;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getUserId(Entry entry) {
        return entry.getValue(0, (String) null);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getPassword(Entry entry) {
        return entry.getValue(1, (String) null);
    }

    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param type _more_
     * @param subId _more_
     *
     * @return _more_
     */
    public String getSynthId(Entry parentEntry, String type, String subId) {
        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + type
               + ":" + subId;
    }

    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param subId _more_
     *
     * @return _more_
     */
    public String getSynthId(Entry parentEntry, String subId) {
        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + subId;
    }



}
