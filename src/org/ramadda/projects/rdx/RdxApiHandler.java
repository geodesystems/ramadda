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

package org.ramadda.projects.rdx;


import org.ramadda.plugins.phone.TwilioApiHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * Provides an API for handling station changes
 * The file api.xml specifies the /path to method mapping
 */
public class RdxApiHandler extends RepositoryManager implements RequestHandler {

    /** _more_ */
    private int passwordErrors = 0;

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public RdxApiHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param message _more_
     *
     * @return _more_
     */
    private Result makeErrorResult(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(Json.map("status", "false", "message",
                           Json.quote(message)));

        return new Result(sb.toString(), "application/json");
    }


    /**
     * handle the site update request
     *
     * @param request request
     *
     * @return JSON status
     *
     * @throws Exception on badness
     */
    public Result processUpdate(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        //If too many failed attempts then bail
        if (passwordErrors > 50) {
            return makeErrorResult("Too many failed requests");
        }

        String auth = getRepository().getProperty("rdx.update.password");
        if (auth == null) {
            return makeErrorResult(
                "No rdx.update.password specified in repository properties");
        }

        String password = request.getString("password", (String) null);
        if (password == null) {
            return makeErrorResult("No password specified in request");
        }

        if ( !password.equals(auth)) {
            //Sleep a second to slow down multiple guesses
            Misc.sleepSeconds(1);
            passwordErrors++;

            return makeErrorResult("Incorrect password specified in request");
        }

        //Clear the error count
        passwordErrors = 0;

        String id = request.getString("instrument_id", (String) null);
        if (id == null) {
            return makeErrorResult("No instrument_id specified in request");
        }


        //Find the station entries
        Request tmpRequest = getRepository().getTmpRequest();
        tmpRequest.put("type", "rdx_instrument");
        tmpRequest.put("search.rdx_instrument.instrument_id", id);
        List[]      result  = getEntryManager().getEntries(tmpRequest);
        List<Entry> entries = new ArrayList<Entry>();
        entries.addAll((List<Entry>) result[0]);
        entries.addAll((List<Entry>) result[1]);
        if (entries.size() == 0) {
            return makeErrorResult("Could not find instrument: " + id);
        }

        String  message    = "";
        boolean haveStatus = request.defined("network_status");
        boolean haveData   = request.defined("data_download");
        int     data       = request.get("data_download", 0);
        boolean network    = request.get("network_status", true);
        for (Entry entry : entries) {
            boolean needToNotify = false;
            boolean changed      = false;
            if (haveStatus) {
                if ((boolean) entry.getValue(
                        RdxInstrumentTypeHandler.IDX_NETWORK_UP) != network) {
                    changed = true;
                    entry.setValue(RdxInstrumentTypeHandler.IDX_NETWORK_UP,
                                   network);
                    if ( !network) {
                        needToNotify = true;
                    }
                }
            }

            if (haveData) {
                if ((int) entry.getValue(
                        RdxInstrumentTypeHandler.IDX_DATA_DOWN) != data) {
                    changed = true;
                    entry.setValue(RdxInstrumentTypeHandler.IDX_DATA_DOWN,
                                   data);
                }
            }

            if (changed) {
                System.err.println("changed:" + entry);
                getEntryManager().updateEntry(request, entry);
                message += "updated: " + entry.getName() + "\n";
            }

            if (needToNotify) {
                String url = request.getAbsoluteUrl(
                                 request.entryUrl(
                                     getRepository().URL_ENTRY_SHOW, entry));
                String msg = "Network for station:" + id + " is down\n" + url;
                try {
                    sendNotification(request, entry, id, msg);
                } catch (Exception exc) {
                    System.err.println(
                        "RdxApiHandler: Error sending notification:" + exc);
                    exc.printStackTrace();
                }
            }
        }
        sb.append(Json.map("status", "true", "message", Json.quote(message)));

        return new Result(sb.toString(), "application/json");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param siteId _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
    private void sendNotification(Request request, Entry entry,
                                  String siteId, String msg)
            throws Exception {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        boolean weekend = (cal.get(cal.DAY_OF_WEEK) == cal.SUNDAY)
                          || (cal.get(cal.DAY_OF_WEEK) == cal.SATURDAY);

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "rdx_notification", true);
        if ((metadataList == null) || (metadataList.size() == 0)) {
            System.err.println("RdxApiHandler: no notifications found");
            return;
        }

        for (Metadata metadata : metadataList) {
            String when = metadata.getAttr4();
            if (when.equals("weekend") && !weekend) {
                continue;
            }
            String name  = metadata.getAttr1();
            String email = Utils.trim(metadata.getAttr2());
            String phone = Utils.trim(metadata.getAttr3());
            System.err.println("RdxApiHandler: notification:" + name
                               + " email:" + email + " phone:" + phone);
            if (email.length() > 0) {
                if ( !getRepository().getMailManager().isEmailCapable()) {
                    System.err.println(
                        "RdxApiHandler: Error: Email is not enabled");

                    continue;
                }
                System.err.println(
                    "RdxApiHandler: Sending site status email:" + email);
                getRepository().getMailManager().sendEmail(email,
                        "Site status:" + siteId, msg, true);
            }

            phone = phone.replaceAll("-", "");
            if (phone.length() > 0) {
                TwilioApiHandler twilio =
                    (TwilioApiHandler) getRepository().getApiManager()
                        .getApiHandler("twilio");
                if ((twilio == null) || !twilio.sendingEnabled()) {
                    System.err.println(
                        "RdxApiHandler: Error: SMS is not enabled");

                    continue;
                }
                System.err.println("RdxApiHandler: Sending site status sms:"
                                   + phone);
                twilio.sendTextMessage(null, phone, msg);
            }
        }
    }
}
