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

package org.ramadda.repository.monitor;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.List;


/**
 * Class FileInfo _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class SynchronousEntryMonitor extends EntryMonitor {

    /** _more_ */
    private Entry entry;


    /**
     * _more_
     */
    public SynchronousEntryMonitor() {}


    /**
     * _more_
     *
     * @param repository _more_
     * @param request _more_
     */
    public SynchronousEntryMonitor(Repository repository, Request request) {
        super(repository, request.getUser(), "Synchronous Search", false);
        Hashtable properties = request.getDefinedProperties();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) properties.get(arg);
            addFilter(new Filter(arg, value));
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "Synchronous Action";
    }

    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void entryMatched(Entry entry) {
        this.entry = entry;
        synchronized (this) {
            this.notify();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Entry getEntry() {
        return entry;
    }

}
