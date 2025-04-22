/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class NoopAction extends MonitorAction {

    public NoopAction() {}

    public NoopAction(String id) {
        super(id);
    }

    public String getActionLabel() {
        return "Noop";
    }

    public String getActionName() {
        return "noop";
    }

    public String getSummary(EntryMonitor entryMonitor) {
        return "noop";
    }

    @Override
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {}

}
