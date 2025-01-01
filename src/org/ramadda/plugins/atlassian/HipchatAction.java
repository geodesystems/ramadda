/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.monitor.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class HipchatAction extends MonitorAction {

    /** _more_ */
    public static final String ARG_WEBHOOK = "webhook";

    /** _more_ */
    public static final String ARG_PUBLISH_FILE = "publish_file";


    /** _more_ */
    private String webhook;

    /** _more_ */
    private boolean publishFile = false;


    /**
     * _more_
     */
    public HipchatAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public HipchatAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getActionName() {
        return "Hipchat Action";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getActionLabel() {
        return "Hipchat Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    @Override
    public String getSummary(EntryMonitor entryMonitor) {
        return "Post a link to Hipchat";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getInitialMessageTemplate() {
        return "New RAMADDA entry: ${name} ${url}";
    }

    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.colspan("Post a link to the entry in Hipchat",
                                    2));
        sb.append(HtmlUtils.formEntry("Hipchat Web Hook URL:",
                                      HtmlUtils.input(ARG_WEBHOOK,
                                          getWebhook(), HtmlUtils.SIZE_60)));
        /*
        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    ARG_PUBLISH_FILE, "true", getPublishFile()) + " "
                        + monitor.getRepository().msg(
                            "Publish file to Hipchat")));
        */
        sb.append(HtmlUtils.formTableClose());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.webhook     = request.getString(ARG_WEBHOOK, webhook);
        this.publishFile = request.get(ARG_PUBLISH_FILE, publishFile);
    }

    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     * @param isNew _more_
     */
    @Override
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
        try {
            super.entryMatched(monitor, entry, isNew);
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            Hipchat.makeEntryResult(monitor.getRepository(),
                                    new Request(monitor.getRepository(),
                                        null), (isNew
                    ? "New"
                    : "Modified") + " "
                                  + entry.getTypeHandler()
                                      .getLabel(), entries,
                                          getWebhookToUse(monitor
                                              .getRepository()), false);
        } catch (Exception exc) {
            monitor.handleError("Error posting to Monitor   ", exc);
        }
    }


    /**
     *  Set the Webhook property.
     *
     *  @param value The new value for Webhook
     */
    public void setWebhook(String value) {
        webhook = value;
    }

    /**
     *  Get the Webhook property.
     *
     *  @return The Webhook
     */
    public String getWebhook() {
        return webhook;
    }


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @return _more_
     */
    public String getWebhookToUse(Repository repository) {
        if ( !Utils.stringDefined(webhook)) {
            return null;
        }
        String fromProp = repository.getProperty(webhook, (String) null);
        if (fromProp != null) {
            return fromProp;
        }

        return webhook;

    }


    /**
     *  Set the PublishFile property.
     *
     *  @param value The new value for PublishFile
     */
    public void setPublishFile(boolean value) {
        publishFile = value;
    }

    /**
     *  Get the PublishFile property.
     *
     *  @return The PublishFile
     */
    public boolean getPublishFile() {
        return publishFile;
    }



}
