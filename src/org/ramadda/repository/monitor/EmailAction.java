/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

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
public class EmailAction extends PasswordAction {

    public EmailAction() {}

    public EmailAction(String id) {
        super(id);
    }

    public EmailAction(String id, String remoteUserId) {
        super(id, remoteUserId, (String) null);
    }

    public String getActionLabel() {
        return "Email Action";
    }

    public String getActionName() {
        return "email";
    }

    public String getSummary(EntryMonitor entryMonitor) {
        return "Send an email to " + getRemoteUserId();
    }

    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.colspan("Send an email", 2));

        sb.append(
            HtmlUtils.formEntry(
                "Email address",
                HtmlUtils.input(
                    getArgId(ARG_ACTION_ID), getRemoteUserId(),
                    HtmlUtils.SIZE_60)));
        sb.append(
            HtmlUtils.formEntryTop(
                "Message",
                HtmlUtils.textArea(
                    getArgId(ARG_ACTION_MESSAGE), getMessageTemplate(), 5,
                    60)));
        sb.append(HtmlUtils.formTableClose());
    }

    @Override
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
        try {
            String from = monitor.getUser().getEmail();
            if ((from == null) || (from.trim().length() == 0)) {
                from = monitor.getRepository().getProperty(PROP_ADMIN_EMAIL,
                        "");
            }

            try {
                for (String to :
                        Utils.split(getRemoteUserId(), ",", true, true)) {
                    monitor.getRepository().getLogManager().logInfo(
                        "Monitor:" + this + " sending mail to: " + to);
                    String message = getMessage(monitor, entry);
                    monitor.getRepository().getMailManager().sendEmail(to,
                            from, "New Entry", message, false);
                }
            } catch (Exception exc) {
                monitor.handleError("Error sending email to "
                                    + getRemoteUserId() + " from:"
                                    + from, exc);
            }
        } catch (Exception exc2) {
            monitor.handleError("Error:", exc2);
        }
    }

}
