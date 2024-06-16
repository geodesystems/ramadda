/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import java.util.GregorianCalendar;
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
    public Result processImage(Request request) throws Exception {
        Request adminRequest = getRepository().getAdminRequest();
        Entry entry = getEntryManager().getEntry(adminRequest,
                          request.getString("entryid", ""));
        if (entry == null) {
            throw new IllegalArgumentException("no entry found");
        }
        if ( !entry.getTypeHandler().isType("phone_mttf")) {
            throw new IllegalArgumentException("bad type");
        }
        if ( !entry.getResource().isImage()) {
            throw new IllegalArgumentException("not an image");
        }
        String path = entry.getResource().getPath();
        String mimeType = getRepository().getMimeTypeFromSuffix(path);

        String fileName = getStorageManager().getFileTail(entry);
        InputStream inputStream =
            getStorageManager().getFileInputStream(entry.getFile());
        Result result = new Result(BLANK, inputStream, mimeType);
        result.setReturnFilename(fileName);

        return result;
    }

    /**
     * _more_
     */
    private void runMessages() {
        Misc.sleepSeconds(30);
        if ( !repository.getProperty("messages.enabled", false)) {
            return;
        }
        while (true) {
            try {
                if ( !repository.getProperty("messages.enabled", false)) {
                    return;
                }
                checkMessages();
            } catch (Exception exc) {
                System.err.println("MessageApiHandler got error:" + exc);

                break;
            }
            //            Misc.sleepSeconds(30);
            Misc.sleepSeconds(repository.getProperty("messages.timeout",
                    60 * 5));
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
        List<Entry> entries =
            getRepository().getEntryManager().getEntriesFromDb(request);
        Date now = new Date();
        for (Entry entry : entries) {
            if ( !entry.getBooleanValue(request,MTTFTypeHandler.IDX_ENABLED, false)) {
                //                System.err.println("\tnot enabled");
                continue;
            }
            System.err.println("MessageApiHandler: entry: "
                               + entry.getName());
            if (entry.getStartDate() > entry.getEndDate()) {
                System.err.println("\tpast time");
                entry.setValue(MTTFTypeHandler.IDX_ENABLED,
                               Boolean.valueOf(false));
                getEntryManager().updateEntry(request, entry);

                continue;
            }
            if (entry.getStartDate() >= now.getTime()) {
                System.err.println("\tnot ready");

                continue;
            }
            String originalStatus =
                entry.getStringValue(request,MTTFTypeHandler.IDX_STATUS, "");
            boolean[] sent         = { false };
            boolean   inError      = false;
            boolean   needToUpdate = false;
            String    status       = null;

            try {
                status = processMessage(request, twilio, entry, sent);
            } catch (Exception exc) {
                inError = true;
                status  = exc.getMessage();
            }

            if (sent[0]) {
                String recurrence =
                    entry.getStringValue(request,MTTFTypeHandler.IDX_RECURRENCE, "").trim();
                double value =
                    entry.getDoubleValue(request,MTTFTypeHandler.IDX_RECURRENCE_VALUE, 0.0);
                if (Misc.equals(recurrence, "days") && (value > 0)) {
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTimeInMillis(now.getTime());
                    int offset = (int) (value * 24);
                    cal.add(cal.HOUR, offset);
                    entry.setStartDate(cal.getTimeInMillis());
                    System.err.println(" next:"
                                       + new Date(entry.getStartDate()));
                } else if (Misc.equals(recurrence, "dayofmonth")
                           && (value > 0)) {
                    int               dom = (int) value;
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTimeInMillis(now.getTime());
                    cal.add(cal.MONTH, 1);
                    cal.add(cal.DAY_OF_MONTH, dom);
                    entry.setStartDate(cal.getTimeInMillis());
                    System.err.println(" next:"
                                       + new Date(entry.getStartDate()));
                } else {
                    entry.setValue(MTTFTypeHandler.IDX_ENABLED,
                                   Boolean.valueOf(false));
                }
                needToUpdate = true;
            }

            if (inError) {
                entry.setValue(MTTFTypeHandler.IDX_ENABLED,
                               Boolean.valueOf(false));
                needToUpdate = true;
            }
            if (entry.getStartDate() > entry.getEndDate()) {
                entry.setValue(MTTFTypeHandler.IDX_ENABLED,
                               Boolean.valueOf(false));
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
                getEntryManager().updateEntry(request, entry);
            }
        }

    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param twilio _more_
     * @param entry _more_
     * @param sent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String processMessage(Request request, TwilioApiHandler twilio,
                                  Entry entry, boolean[] sent)
            throws Exception {
        String subject = entry.getStringValue(request,MTTFTypeHandler.IDX_SUBJECT,
                                        "").trim();
        String fromEmail = entry.getStringValue(request,MTTFTypeHandler.IDX_FROM_EMAIL,
                                          "").trim();
        List<String> toEmail =
            StringUtil.split(entry.getStringValue(request,MTTFTypeHandler.IDX_TO_EMAIL,
                                            ""), ",", true, true);
        List<String> toPhone =
            StringUtil.split(entry.getStringValue(request,MTTFTypeHandler.IDX_TO_PHONE,
                                            ""), ",", true, true);
        String message = entry.getStringValue(request,MTTFTypeHandler.IDX_MESSAGE, "");


        if (toPhone.size() > 0) {
            if (twilio == null) {
                return "SMS not enabled";
            }
            String url = null;
            if (entry.getResource().isImage()) {
                url = request.getAbsoluteUrl(
                    getRepository().getUrlBase()
                    + "/phone/message/image?entryid=" + entry.getId());
                System.err.println("url:" + url);
            }
            for (String phone : toPhone) {
                twilio.sendTextMessage(null, phone, message, url);
            }
            sent[0] = true;

            return "SMS sent @ "
                   + getRepository().getDateHandler().formatDate(new Date());
        }
        if (toEmail.size() > 0) {
            if ( !getMailManager().isEmailEnabled()) {
                return "Email not enabled";
            }
            if ( !Utils.stringDefined(fromEmail)) {
                return "Need to specify a from email";
            }
            sent[0] = true;
            for (String email : toEmail) {
                getRepository().getMailManager().sendEmail(email, fromEmail,
                        subject, message, false, entry.getFile());
            }

            return "Email sent @ " + new Date();
        }

        return "";

    }


}
