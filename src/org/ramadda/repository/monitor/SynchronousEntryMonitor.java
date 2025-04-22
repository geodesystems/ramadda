/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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

    private Entry entry;

    public SynchronousEntryMonitor() {}

    public SynchronousEntryMonitor(Repository repository, Request request) {
        super(repository, request.getUser(), "Synchronous Search", false);
        Hashtable properties = request.getDefinedProperties();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) properties.get(arg);
            addFilter(new Filter(arg, value));
        }
    }

    public String getActionName() {
        return "Synchronous Action";
    }

    protected void entryMatched(Entry entry) {
        this.entry = entry;
        synchronized (this) {
            this.notify();
        }
    }

    public Entry getEntry() {
        return entry;
    }

}
