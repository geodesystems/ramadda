/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.phone;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;
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
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
@SuppressWarnings("unchecked")
public class TwilioApiHandler extends RepositoryManager implements RequestHandler {

    /** _more_ */
    public static final String PROP_AUTHTOKEN = "twilio.authtoken";

    /** _more_ */
    public static final String PROP_PHONE = "twilio.phone";

    /** _more_ */
    public static final String PROP_ACCOUNTSID = "twilio.accountsid";

    /** _more_ */
    public static final String PROP_TRANSCRIBE = "twilio.transcribe";


    /** _more_ */
    public static final String ARG_ACCOUNTSID = "AccountSid";

    /** _more_ */
    public static final String ARG_RECORDINGSID = "RecordingSid";

    /** _more_ */
    public static final String ARG_RECORDINGURL = "RecordingUrl";

    /** _more_ */
    public static final String ARG_FROMZIP = "FromZip";


    /** _more_ */
    public static final String XML_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /** _more_ */
    public static final String TAG_RESPONSE = "Response";

    /** _more_ */
    public static final String TAG_RECORD = "Record";

    /** _more_ */
    public static final String TAG_TRANSCRIPTIONTEXT = "TranscriptionText";

    /** _more_ */
    public static final String TAG_STATUS = "Status";

    /** _more_ */
    public static final String TAG_SMS = "Sms";

    /** _more_ */
    public static final String TAG_SAY = "Say";

    /** _more_ */
    public static final String ATTR_VOICE = "voice";

    /** _more_ */
    public static final String ATTR_TRANSCRIBE = "transcribe";

    /** _more_ */
    public static final String ATTR_TO = "to";

    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ARG_FROM = "From";

    /** _more_ */
    public static final String ARG_TO = "To";

    /** _more_ */
    public static final String ARG_BODY = "Body";

    /** _more_ */
    public static final String ARG_ = "";
    //    public static final String ARG_  = ""; 

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public TwilioApiHandler(Repository repository) throws Exception {
        super(repository);
        String appId = getSid();
        if (appId != null) {
            repository.getAccessManager().setTwoFactorAuthenticator(
                new TwilioTwoFactorAuthenticator(repository) {}
            );
        }

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean sendingEnabled() {
        return isEnabled()
               && Utils.stringDefined(getRepository().getProperty(PROP_PHONE,
                   ""));
    }

    /**
     *  @return _more_
     */
    private String getSid() {
        return getRepository().getProperty(
            PROP_ACCOUNTSID,
            getRepository().getProperty("twilio.appid", null));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return Utils.stringDefined(getSid());
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class TwilioTwoFactorAuthenticator extends AccessManager
        .TwoFactorAuthenticator {

        /** _more_ */
        private Repository repository;

        /**
         * _more_
         *
         * @param repository _more_
         */
        public TwilioTwoFactorAuthenticator(Repository repository) {
            this.repository = repository;
            authCache       = new TTLCache<String, String>(1000 * 60 * 10);
        }

        /** _more_ */
        private TTLCache<String, String> authCache;


        /**
         * _more_
         *
         * @param user _more_
         *
         * @return _more_
         */
        @Override
        public boolean userCanBeAuthenticated(User user) {
            System.err.println("auth user:" + user);
            String phone = (String) user.getProperty("phone");
            if (phone == null) {
                return false;
            }
            phone = phone.replaceAll("[^\\d]", "");
            if (phone.length() != 10) {
                return false;
            }

            return true;
        }

        /**
         * _more_
         *
         * @param request _more_
         * @param user _more_
         * @param sb _more_
         *
         * @throws Exception _more_
         */
        @Override
        public void addAuthForm(Request request, User user, Appendable sb)
                throws Exception {
            String phone = (String) user.getProperty("phone");
            if (phone == null) {
                sb.append(
                    repository.getPageHandler().showDialogNote(
                        "No phone number on record"));

                return;
            }
            phone = phone.replaceAll("[^\\d]", "");
            String from   = getRepository().getProperty(PROP_PHONE, "");
            double random = 1000000 * Math.random();
            String code   = "" + random;
            code = StringUtil.padRight(code.substring(0, 6), 6, "0");
            authCache.put(user.getId(), code);
            sendTextMessage(from, phone, "RAMADDA login code:\n" + code);

            sb.append(
                repository.getPageHandler().showDialogNote(
                    "Enter the code we texted you"));

            String id = request.getString(ARG_USER_ID, "");
            sb.append(HtmlUtils.formPost(repository.getUrlPath(request,
                    repository.getRepositoryBase().URL_USER_LOGIN)));



            sb.append(HtmlUtils.formTable());
            sb.append(HtmlUtils.formEntry(msgLabel("Code"),
                                          HtmlUtils.input("code", "")));
            if (request.exists(ARG_REDIRECT)) {
                sb.append(HtmlUtils.hidden(ARG_REDIRECT,
                                           request.getString(ARG_REDIRECT,
                                               "")));
            }
            sb.append(HtmlUtils.hidden(ARG_USER_ID, user.getId()));
            sb.append(
                HtmlUtils.formEntry(
                    "",
                    HtmlUtils.submit(LABEL_LOGIN) + " "
                    + HtmlUtils.submit("Resend Code")));
            sb.append(HtmlUtils.formClose());
            sb.append(HtmlUtils.formTableClose());
        }


        /**
         * _more_
         *
         * @param request _more_
         * @param user _more_
         * @param sb _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public boolean userHasBeenAuthenticated(Request request, User user,
                Appendable sb)
                throws Exception {
            String enteredCode = request.getString("code", "");
            String sentCode    = authCache.get(user.getId());
            System.err.println("codes:" + enteredCode + "  " + sentCode);

            return Misc.equals(enteredCode, sentCode);
        }

    }




    /**
     * _more_
     *
     * @return _more_
     */
    public List<PhoneHarvester> getHarvesters() {
        List<PhoneHarvester> harvesters = new ArrayList<PhoneHarvester>();
        for (Harvester harvester : getHarvesterManager().getHarvesters()) {
            if (harvester.getActiveOnStart()
                    && (harvester instanceof PhoneHarvester)) {
                harvesters.add((PhoneHarvester) harvester);
            }
        }

        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                PhoneHarvester ph1     = (PhoneHarvester) o1;
                PhoneHarvester ph2     = (PhoneHarvester) o2;
                int            weight1 = ph1.getWeight();
                int            weight2 = ph2.getWeight();
                if (weight1 == weight2) {
                    return 0;
                }
                if (weight1 <= weight2) {
                    return 1;
                }

                return -1;
            }
        };
        Collections.sort(harvesters, comp);

        return harvesters;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    private boolean callOK(Request request) {
        String appId = getSid();
        if (appId == null) {
            return false;
        }

        return request.getString(ARG_ACCOUNTSID, "").equals(appId);
    }


    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processSms(Request request) throws Exception {
        PhoneInfo info = new PhoneInfo(PhoneInfo.TYPE_SMS,
                                       request.getString(ARG_FROM, ""),
                                       request.getString(ARG_TO, ""), null);
        //        System.err.println("TwilioApiHandler: request: " + request);
        StringBuffer sb = new StringBuffer(XML_HEADER);
        sb.append(XmlUtil.openTag(TAG_RESPONSE));
        info.setMessage(request.getString(ARG_BODY, ""));
        info.setFromZip(request.getString(ARG_FROMZIP, (String) null));
        boolean handledMessage = false;
        if ( !callOK(request)) {
            sb.append(XmlUtil.tag(TAG_SMS, "",
                                  "Sorry, bad APPID property defined"));
        } else {
            StringBuffer msg = new StringBuffer();
            for (PhoneHarvester harvester : getHarvesters()) {
                if (harvester.handleMessage(request, info, msg)) {
                    String response = msg.toString();
                    if (response.length() == 0) {
                        response = "OK";
                    }

                    int cnt = 0;
                    List<String> lines = StringUtil.split(response, "\n",
                                             false, false);
                    //                    System.err.println(response);
                    StringBuffer buff = new StringBuffer();
                    for (String line : lines) {
                        if (buff.length() + line.length() > 159) {
                            if (buff.length() > 0) {
                                sb.append(XmlUtil.tag(TAG_SMS, "",
                                        XmlUtil.getCdata(buff.toString())));
                                if (cnt++ > 4) {
                                    break;
                                }
                            }
                            buff = new StringBuffer(line);
                        } else {
                            if (buff.length() > 0) {
                                buff.append("\n");
                            }
                            buff.append(line);
                        }
                    }
                    if (buff.length() > 0) {
                        sb.append(XmlUtil.tag(TAG_SMS, "",
                                XmlUtil.getCdata(buff.toString())));
                    }
                    handledMessage = true;

                    break;
                }
            }

            //            System.err.println("xml:" + sb);


            if ( !handledMessage) {
                String response = msg.toString();
                sb.append(
                    XmlUtil.tag(
                        TAG_SMS, "",
                        "Sorry, RAMADDA was not able to process your message.\n"
                        + response));
            }
        }
        sb.append(XmlUtil.closeTag(TAG_RESPONSE));

        return new Result("", sb, "text/xml");
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
    public Result sendText(Request request) throws Exception {
        //TODO
        StringBuffer sb = new StringBuffer();

        return new Result("", sb);
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
    public Result processVoice(Request request) throws Exception {

        String authToken = getRepository().getProperty(PROP_AUTHTOKEN, null);
        String recordingUrl = request.getString(ARG_RECORDINGURL, null);
        System.err.println("processVoice:" + request.getUrlArgs());
        StringBuffer sb = new StringBuffer();
        sb.append(XML_HEADER);
        sb.append(XmlUtil.openTag(TAG_RESPONSE));
        try {
            PhoneInfo info = new PhoneInfo(PhoneInfo.TYPE_SMS,
                                           request.getString(ARG_FROM, ""),
                                           request.getString(ARG_TO, ""),
                                           null);
            if ( !callOK(request)) {
                sb.append(XmlUtil.tag(TAG_SAY,
                                      XmlUtil.attr(ATTR_VOICE, "woman"),
                                      "Sorry, bad application identifier"));
            } else {
                if (recordingUrl == null) {
                    String  voiceResponse = null;
                    boolean canEdit       = true;
                    for (PhoneHarvester harvester : getHarvesters()) {
                        String response = harvester.getVoiceResponse(info);
                        if ((response != null)
                                && (response.trim().length() > 0)) {
                            voiceResponse = response;
                            canEdit       = harvester.canEdit(info);

                            break;
                        }
                    }
                    if (voiceResponse == null) {
                        sb.append(XmlUtil.tag(TAG_SAY,
                                XmlUtil.attr(ATTR_VOICE, "woman"),
                                "Sorry, this ramadda repository does not accept voice messages"));
                    } else if ( !canEdit) {
                        sb.append(XmlUtil.tag(TAG_SAY,
                                XmlUtil.attr(ATTR_VOICE, "woman"),
                                "Sorry, you need to login through a text message first"));
                    } else {
                        sb.append(XmlUtil.tag(TAG_SAY,
                                XmlUtil.attr(ATTR_VOICE, "woman"),
                                voiceResponse));
                        String recordAttrs = XmlUtil.attrs(new String[] {
                                                 "maxLength",
                                "30", });

                        if (getRepository().getProperty(PROP_TRANSCRIBE,
                                false)) {
                            recordAttrs += XmlUtil.attr(ATTR_TRANSCRIBE,
                                    "true");
                        }
                        sb.append(XmlUtil.tag(TAG_RECORD, recordAttrs));
                    }
                } else {
                    info.setRecordingUrl(recordingUrl);
                    if (getRepository().getProperty(PROP_TRANSCRIBE, false)) {
                        int    cnt  = 0;
                        String text = null;
                        while (cnt++ < 5) {
                            text = getTranscriptionText(request, authToken);
                            if (text != null) {
                                break;
                            }
                            Misc.sleepSeconds(3);
                        }
                        if (text != null) {
                            info.setTranscription(text);
                        } else {
                            System.err.println(
                                "processVoice: failed to get transcription text");
                        }
                    }

                    for (PhoneHarvester harvester : getHarvesters()) {
                        StringBuffer msg = new StringBuffer();
                        if (harvester.handleVoice(request, info, msg)) {
                            if (msg.length() > 0) {
                                sendTextMessage(info.getToPhone(),
                                        info.getFromPhone(), msg.toString());
                            }

                            break;
                        }
                    }

                }
            }
        } catch (Exception exc) {
            sb = new StringBuffer();
            sb.append(XML_HEADER);
            sb.append(XmlUtil.openTag(TAG_RESPONSE));
            sb.append(XmlUtil.tag(TAG_SAY, XmlUtil.attr(ATTR_VOICE, "woman"),
                                  "Sorry, an error occurred"));
            exc.printStackTrace();
            getLogManager().logError("Error handling twilio voice message",
                                     exc);
        }
        sb.append(XmlUtil.closeTag(TAG_RESPONSE));
        System.err.println("voice response:" + sb);

        return new Result("", sb, "text/xml");

    }


    /**
     * _more_
     *
     * @param fromPhone _more_
     * @param toPhone _more_
     * @param msg _more_
     *
     *  @return _more_
     * @throws Exception _more_
     */
    public boolean sendTextMessage(String fromPhone, String toPhone,
                                   String msg)
            throws Exception {
        return sendTextMessage(fromPhone, toPhone, msg, null);
    }

    /**
     * _more_
     *
     * @param fromPhone _more_
     * @param toPhone _more_
     * @param msg _more_
     * @param url _more_
     *
     *  @return _more_
     * @throws Exception _more_
     */
    public boolean sendTextMessage(String fromPhone, String toPhone,
                                   String msg, String url)
            throws Exception {
        if ( !isEnabled()) {
            System.err.println(
                "Twilio sendTextMessage not enabled - no appid specified");

            return false;
        }
        if (fromPhone == null) {
            fromPhone = getRepository().getProperty(PROP_PHONE, "");
        }
        fromPhone = fromPhone.replaceAll("-", "").replaceAll(" ", "");

        if ( !Utils.stringDefined(fromPhone)) {
            System.err.println(
                "Twilio sendTextMessage not enabled - no from phone specified");

            return false;
        }


        toPhone = toPhone.replaceAll("-", "").replaceAll(" ", "");

        if ( !fromPhone.startsWith("+1")) {
            fromPhone = "+1" + fromPhone;
        }
        if ( !toPhone.startsWith("+1")) {
            toPhone = "+1" + toPhone;
        }
        String appId     = getSid();
        String authToken = getRepository().getProperty(PROP_AUTHTOKEN, null);
        String smsUrl = "https://api.twilio.com/2010-04-01/Accounts/" + appId
                        + "/Messages.json";
        //        -u xxxxxx:[AuthToken]

        String result = doPost(smsUrl,
                               "From="
                               + URLEncoder.encode(fromPhone, "UTF-8") + "&"
                               + "To=" + URLEncoder.encode(toPhone, "UTF-8")
                               + ((url == null)
                                  ? ""
                                  : "&" + "MediaUrl="
                                    + URLEncoder.encode(url, "UTF-8")) + "&"
                                        + "Body="
                                        + URLEncoder.encode(msg, "UTF-8"));

        //      System.err.println("Twilio result:" + result);
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getApiPrefix() {
        return "https://api.twilio.com/2010-04-01/Accounts/" + getSid();

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param authToken _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getTranscriptionText(Request request, String authToken)
            throws Exception {
        String transcriptionUrl = getApiPrefix() + "/Recordings/"
                                  + request.getString(ARG_RECORDINGSID, null)
                                  + "/Transcriptions";
        URL               url = new URL(transcriptionUrl);
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        String auth = request.getString(ARG_ACCOUNTSID, null) + ":"
                      + authToken;
        String encoding = Utils.encodeBase64(auth);
        huc.addRequestProperty("Authorization", "Basic " + encoding);
        String transcription =
            new String(IOUtil.readBytes(huc.getInputStream()));
        Element root       = XmlUtil.getRoot(transcription);
        Element statusNode = XmlUtil.findDescendant(root, TAG_STATUS);
        if (statusNode == null) {
            return null;
        }
        if ( !XmlUtil.getChildText(statusNode).equals("completed")) {
            return null;

        }

        Element textNode = XmlUtil.findDescendant(root,
                               TAG_TRANSCRIPTIONTEXT);
        if (textNode != null) {
            return XmlUtil.getChildText(textNode);
        }

        return null;
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String doPost(String url, String args) throws Exception {
        URL               myurl = new URL(url);
        HttpURLConnection huc   = (HttpURLConnection) myurl.openConnection();
        String auth = getSid() + ":"
                      + getRepository().getProperty(PROP_AUTHTOKEN, null);
        String encoding = Utils.encodeBase64(auth);
        huc.addRequestProperty("Authorization", "Basic " + encoding);

        huc.setRequestMethod("POST");
        huc.setRequestProperty("Host", "api.twilio.com");
        //        huc.setRequestProperty("Content-length",
        //                               String.valueOf(args.length()));
        huc.setDoOutput(true);
        huc.setDoInput(true);
        DataOutputStream output = new DataOutputStream(huc.getOutputStream());
        output.writeBytes(args);
        output.close();

        try {
            return new String(IOUtil.readBytes(huc.getInputStream(), null));
        } catch (Exception exc) {
            String result = new String(IOUtil.readBytes(huc.getErrorStream(),
                                null));
            System.err.println("TwilioApiHandler error:"
                               + huc.getResponseMessage());
            System.err.println(result);

            throw new IllegalArgumentException(result);
        }
    }







}
