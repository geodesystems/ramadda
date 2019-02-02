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

package org.ramadda.repository.harvester;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Utils;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;



import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class MonitorHarvester extends Harvester {

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_EMAILS = "emails";

    /** _more_ */
    public static final String ATTR_FAILURES = "failures";

    /** _more_ */
    public static final String ATTR_MINUTES = "minutes";

    /** _more_ */
    public static final String ATTR_MESSAGE = "message";

    /** _more_ */
    private List<String> patternNames = new ArrayList<String>();


    /** _more_ */
    private String url = "";

    /** _more_ */
    private String emails = "";

    /** _more_ */

    private int failures = 2;

    /** _more_ */
    private int minutes = 1;

    /** _more_ */
    private String message =
        "The server at the following URL did not respond\n${url}";


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public MonitorHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public MonitorHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }




    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    @Override
    protected void init(Element element) throws Exception {
        super.init(element);
        emails   = XmlUtil.getAttribute(element, ATTR_EMAILS, emails);
        url      = XmlUtil.getAttribute(element, ATTR_URL, url);
        message  = XmlUtil.getAttribute(element, ATTR_MESSAGE, message);
        failures = XmlUtil.getAttribute(element, ATTR_FAILURES, failures);
        minutes  = XmlUtil.getAttribute(element, ATTR_MINUTES, minutes);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getDescription() {
        return "Web Site Monitor";
    }


    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_URL, url);
        element.setAttribute(ATTR_MESSAGE, message);
        element.setAttribute(ATTR_EMAILS, emails);
        element.setAttribute(ATTR_FAILURES, "" + failures);
        element.setAttribute(ATTR_MINUTES, "" + minutes);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        url      = request.getString(ATTR_URL, url).trim();
        message  = request.getString(ATTR_MESSAGE, message);
        emails   = request.getString(ATTR_EMAILS, emails).trim();
        failures = request.get(ATTR_FAILURES, failures);
        minutes  = request.get(ATTR_MINUTES, minutes);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);
        sb.append(HtmlUtils.formEntry("", "Check the URL:"));
        sb.append(HtmlUtils.formEntry(msgLabel("URL"),
                                      HtmlUtils.input(ATTR_URL, url,
                                          HtmlUtils.SIZE_60)));
        sb.append(
            HtmlUtils.formEntry(
                "", "If it fails this number of times in a row"));
        sb.append(HtmlUtils.formEntry(msgLabel("Failed attempts"),
                                      HtmlUtils.input(ATTR_FAILURES,
                                          "" + failures)));
        sb.append(
            HtmlUtils.formEntry(
                "", "Sleeping this number of minutes between attempts"));
        sb.append(HtmlUtils.formEntry(msgLabel("Minutes"),
                                      HtmlUtils.input(ATTR_MINUTES,
                                          "" + minutes)));


        sb.append(HtmlUtils.formEntry("",
                                      "Then send a message to these emails"));

        sb.append(HtmlUtils.formEntry(msgLabel("Emails"),
                                      HtmlUtils.textArea(ATTR_EMAILS, emails,
                                          5, 60) + " One per line."));
        sb.append(HtmlUtils.formEntry(msgLabel("Message"),
                                      HtmlUtils.textArea(ATTR_MESSAGE,
                                          message, 5, 60)));
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        return status.toString();
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Override
    protected void runHarvester() throws Exception {
        resetStatus();
        if ( !Utils.stringDefined(this.url)) {
            logStatus("No URL defined");

            return;
        }
        URL    url         = new URL(this.url);
        String errorMsg    = "";
        int    numFailures = 0;
        while (getActive() && (numFailures < failures)) {
            try {
                logStatus("Fetching URL:" + url);
                checkUrl(url);
                logStatus("Fetched " + HtmlUtils.href(this.url, this.url)
                          + "  successfully");

                return;
            } catch (Exception exc) {
                errorMsg = exc.getMessage();
                System.err.println("Error:" + exc);
                numFailures++;
                logStatus("An error has occurred:" + errorMsg);
                if (numFailures < failures) {
                    logStatus("Sleeping for " + (minutes * 60) + " seconds");
                    Misc.sleepSeconds(minutes * 60);
                }

            }
        }
        setActive(false);
        logStatus("Too many failures:" + errorMsg);
        if ( !getRepository().getMailManager().isEmailCapable()) {
            logStatus("Email is not enabled");
        } else {
            String mail = message.replace("${url}", this.url);
            for (String email : StringUtil.split(emails, "\n", true, true)) {
                getRepository().getMailManager().sendEmail(email,
                        "RAMADDA server monitor failed", mail, false);
                logStatus("email sent to:" + email);
            }
        }


    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @throws Exception _more_
     */
    private void checkUrl(URL url) throws Exception {
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            InputStream is = connection.getInputStream();
        } catch (Exception exc) {
            String msg = "An error has occurred";
            if ((connection != null)
                    && (connection instanceof HttpURLConnection)) {
                HttpURLConnection huc = (HttpURLConnection) connection;
                msg = "Response code: " + huc.getResponseCode() + " ";
                try {
                    InputStream err = huc.getErrorStream();
                    //                    String read  = new String(Utils.readBytes(err, 10000));
                    //                    msg += " Message: "  + 
                } catch (Exception ignoreIt) {}
            }

            throw new IOException(msg);
        }
    }


}
