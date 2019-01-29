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

package org.ramadda.plugins.phone;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.net.*;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
public class MessageApiHandler extends RepositoryManager implements RequestHandler {


    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public MessageApiHandler(Repository repository) throws Exception {
        super(repository);
        Misc.run(new Runnable() {
            public void run() {
                runMessages();
            }
        });
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processDummy(Request request) throws Exception {
        return new Result("", new StringBuffer());
    }

    /**
     * _more_
     */
    private void runMessages() {
        Misc.sleepSeconds(30);
        if(!repository.getProperty("messages.enabled",false)) return;
        while (true) {
            try {
                if(!repository.getProperty("messages.enabled",false)) return;
                checkMessages();
            } catch (Exception exc) {
                System.err.println("MessageApiHandler got error:" + exc);
                break;
            }
            Misc.sleepSeconds(repository.getProperty("messages.timeout",60*5));
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void checkMessages() throws Exception {
        TwilioApiHandler twilio =
            (TwilioApiHandler) getRepository().getApiManager().getApiHandler(
                "twilio");
        if (twilio != null) {
            if ( !twilio.sendingEnabled()) {
                twilio = null;
            }
        }

        Request request = getRepository().getAdminRequest();
        request.put(Constants.ARG_TYPE, "phone_mttf");
        StringBuilder tmp = new StringBuilder();
        List[] pair = getRepository().getEntryManager().getEntries(request,
                          tmp);
        List<Entry> entries = pair[1];
        Date        now     = new Date();
        for (Entry entry : entries) {
            System.err.println("MessageApiHandler: entry: "
                               + entry.getName());
            if ( !entry.getValue(MTTFTypeHandler.IDX_ENABLED, false)) {
                System.err.println("\tnot enabled");

                continue;
            }
            if (entry.getStartDate() >= now.getTime()) {
                System.err.println("\tnot ready");
                //                continue;
            }
            String originalStatus =
                entry.getValue(MTTFTypeHandler.IDX_STATUS, "");
            boolean[] sent         = { false };
            boolean   inError      = false;
            boolean   needToUpdate = false;
            String    status       = null;

            try {
                status = processMessage(twilio, entry, sent);
            } catch (Exception exc) {
                inError = true;
                status  = exc.getMessage();
            }

            if (sent[0] || inError) {
                entry.setValue(MTTFTypeHandler.IDX_ENABLED,
                               new Boolean(false));
                needToUpdate = true;
            }

            if (Misc.equals(status, originalStatus)) {
                status = null;
            }
            if (status != null) {
                entry.setValue(MTTFTypeHandler.IDX_STATUS, status);
                needToUpdate = true;
                System.err.println("\tstatus:" + status);
            }

            if (needToUpdate) {
                System.err.println("\tsaving entry");
                getEntryManager().updateEntry(request, entry);
            }
        }
    }

    /**
     * _more_
     *
     * @param twilio _more_
     * @param entry _more_
     * @param sent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String processMessage(TwilioApiHandler twilio, Entry entry,
                                  boolean[] sent)
            throws Exception {
        String subject = entry.getValue(MTTFTypeHandler.IDX_SUBJECT,
                                        "").trim();
        String fromEmail = entry.getValue(MTTFTypeHandler.IDX_FROM_EMAIL,
                                          "").trim();
        String toEmail = entry.getValue(MTTFTypeHandler.IDX_TO_EMAIL,
                                        "").trim();
        String toPhone = entry.getValue(MTTFTypeHandler.IDX_TO_PHONE,
                                        "").trim();
        String message = entry.getValue(MTTFTypeHandler.IDX_MESSAGE, "");

        if (Utils.stringDefined(toPhone)) {
            if (twilio == null) {
                return "SMS not enabled";
            }
            twilio.sendTextMessage(null, toPhone, message);
            sent[0] = true;

            return "SMS sent @ " + new Date();
        }
        if (Utils.stringDefined(toEmail)) {
            if ( !getAdmin().isEmailCapable()) {
                return "Email not enabled";
            }
            if ( !Utils.stringDefined(fromEmail)) {
                return "Need to specify a from email";
            }
            sent[0] = true;
            getRepository().getMailManager().sendEmail(toEmail, fromEmail, subject,
                                       message,false,entry.getFile());

            return "Email sent @ " + new Date();
        }

        return "";

    }


}
