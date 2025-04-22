/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.xml.XmlUtil;

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
public abstract class PasswordAction extends MonitorAction {

    public static final String ARG_ACTION_ID = "action_id";

    public static final String ARG_ACTION_PASSWORD = "action_password";

    public static final String ARG_ACTION_MESSAGE = "action_message";

    private String remoteUserId = "";

    private String password = "";

    protected String messageTemplate = null;

    public PasswordAction() {}

    public PasswordAction(String id) {
        super(id);
    }

    public PasswordAction(String id, String remoteUserId, String password) {
        super(id);
        this.remoteUserId = remoteUserId;
        this.password     = password;
    }

    public String getMessage(EntryMonitor monitor, Entry entry) {
        String message = getMessageTemplate().replace(
                             "${server}",
                             monitor.getRepository().absoluteUrl(
                                 monitor.getRepository().getUrlBase()));

        return monitor.getRepository().getEntryManager().replaceMacros(entry,
                message);
    }

    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);

        if (request.exists(getArgId(ARG_ACTION_ID))) {
            this.remoteUserId = request.getString(getArgId(ARG_ACTION_ID),
                    remoteUserId);
        }
        if (request.exists(getArgId(ARG_ACTION_PASSWORD))) {
            this.password = request.getString(getArgId(ARG_ACTION_PASSWORD),
                    password);
        }
        if (request.exists(getArgId(ARG_ACTION_MESSAGE))) {
            this.messageTemplate =
                request.getString(getArgId(ARG_ACTION_MESSAGE),
                                  getMessageTemplate());
        }
    }

    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @param value The new value
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(Utils.decodeBase64(new String(value)));
        }
    }

    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @return The Password
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }

        return Utils.encodeBase64(password).getBytes();
    }

    protected String getPassword() {
        return password;
    }

    /**
     * Set the RemoteUserId property.
     *
     * @param value The new value for RemoteUserId
     */
    public void setRemoteUserId(String value) {
        remoteUserId = value;
    }

    /**
     * Get the RemoteUserId property.
     *
     * @return The RemoteUserId
     */
    public String getRemoteUserId() {
        return remoteUserId;
    }

    /**
     * Set the MessageTemplate property.
     *
     * @param value The new value for MessageTemplate
     */
    public void setMessageTemplate(String value) {
        messageTemplate = value;
    }

    /**
     * Get the MessageTemplate property.
     *
     * @return The MessageTemplate
     */
    public String getMessageTemplate() {
        if (messageTemplate == null) {
            messageTemplate = getInitialMessageTemplate();
        }

        return messageTemplate;
    }

    protected String getInitialMessageTemplate() {
        return "A new entry has been created on ${server} by ${user}\n${name} ${url}";
    }

}
