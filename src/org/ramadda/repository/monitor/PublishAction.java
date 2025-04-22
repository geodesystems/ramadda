/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.client.RepositoryClient;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class PublishAction extends MonitorAction {

    public static final String ARG_DESTRAMADDA = "destramadda";

    /**  */
    public static final String ARG_USERID = "userid";

    /**  */
    public static final String ARG_PASSWORD = "password";

    /**  */
    public static final String ARG_PARENTENTRYID = "parententryid";

    private String destRamadda;

    /**  */
    private String userId;

    /**  */
    private String password;

    /**  */
    private String parentEntryId;

    public PublishAction() {}

    public PublishAction(String id) {
        super(id);
    }

    public String getActionName() {
        return "publish";
    }

    public String getActionLabel() {
        return "Publish Action";
    }

    public String getSummary(EntryMonitor entryMonitor) {
        return "Publish entry to: " + (Utils.stringDefined(destRamadda)?destRamadda:"not defined");
    }

    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        destRamadda = request.getString(getArgId(ARG_DESTRAMADDA),
                                        destRamadda).trim();
        userId = request.getString(getArgId(ARG_USERID), userId).trim();
        password = request.getString(getArgId(ARG_PASSWORD), password).trim();
        parentEntryId = request.getString(getArgId(ARG_PARENTENTRYID),
                                          parentEntryId).trim();
    }

    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
	throws Exception {
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.colspan("Publish Action", 2));
        sb.append(
		  HtmlUtils.formEntry(
				      "Destination RAMADDA:",
				      HtmlUtils.input(
						      getArgId(ARG_DESTRAMADDA), destRamadda,
						      HtmlUtils.SIZE_60) + " " + "<br>e.g., https://ramadda.org/repository"));
        sb.append(HtmlUtils.formEntry("User ID:",
                                      HtmlUtils.input(getArgId(ARG_USERID),
						      userId, HtmlUtils.SIZE_40)));
        sb.append(HtmlUtils.formEntry("Password:",
				      HtmlUtils.input(getArgId(ARG_PASSWORD), password,
						      HtmlUtils.SIZE_40) + " "
				      + "<br>Use &quot;property:&lt;property name&gt;&quot; to look up password as a property"));
        sb.append(
		  HtmlUtils.formEntry(
				      "Destination Parent Entry ID:",
				      HtmlUtils.input(
						      getArgId(ARG_PARENTENTRYID), parentEntryId,
						      HtmlUtils.SIZE_40)));
	addPathTemplateEditForm(request, monitor, sb);
        sb.append(HtmlUtils.formTableClose());
    }

    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
        try {
            if ( !Utils.stringDefined(destRamadda)) {
                monitor.logError(this, "no destination RAMADDA specified");
                return;
            }
            if ( !Utils.stringDefined(userId)) {
                monitor.logError(this, "Publish to:" + destRamadda + " no user id specified");

                return;
            }
            if ( !Utils.stringDefined(parentEntryId)) {
                monitor.logError(this,"Publish to:" + destRamadda
				 + " no parent entry id specified");

                return;
            }
            String password = this.password.trim();
            if (password.startsWith("property:")) {
                String key = password.substring("property:".length()).trim();
                password = monitor.getRepository().getProperty(key,
							       (String) null);
                if (password == null) {
                    monitor.logError(this,"Publish to:" + destRamadda
				     + " no password property defined for:" + key);

                    return;
                }
            }

            if ( !Utils.stringDefined(password)) {
                monitor.logError(this,"Publish to:" + destRamadda
				 + " no password specified");

                return;
            }

            Request request = monitor.getRepository().getAdminRequest();
            File file =
                monitor.getRepository().getStorageManager().getTmpFile(".zip");

            request.putExtraProperty("zipfile", file);
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            monitor.getRepository().getZipOutputHandler().toZip(request, "",
								entries, true, true,false);

            String id =
                RepositoryClient.publishToRamadda(new URL(destRamadda),
						 userId, password, parentEntryId, getPathTemplate(),file.toString());
            monitor.logInfo(this, "published to:" + destRamadda + " id:" + id);
        } catch (Exception exc) {
            monitor.handleError("Error handling Publish Action", exc);
        }
    }

    /**
     *  Set the ParentGroupId property.
     *
     *  @param value The new value for ParentGroupId
     */
    public void setDestRamadda(String value) {
        this.destRamadda = value;
    }

    /**
     *  Get the DestRamadda property.
     *
     *  @return The DestRamadda
     */
    public String getDestRamadda() {
        return this.destRamadda;
    }

    /**
     *  Set the UserId property.
     *
     *  @param value The new value for UserId
     */
    public void setUserId(String value) {
        userId = value;
    }

    /**
     *  Get the UserId property.
     *
     *  @return The UserId
     */
    public String getUserId() {
        return userId;
    }

    /**
     *  Set the Password property.
     *
     *  @param value The new value for Password
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     *  Get the Password property.
     *
     *  @return The Password
     */
    public String getPassword() {
        return password;
    }

    /**
     *  Set the ParentEntryId property.
     *
     *  @param value The new value for ParentEntryId
     */
    public void setParentEntryId(String value) {
        parentEntryId = value;
    }

    /**
     *  Get the ParentEntryId property.
     *
     *  @return The ParentEntryId
     */
    public String getParentEntryId() {
        return parentEntryId;
    }

}

