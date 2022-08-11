/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.json.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.net.*;

import java.net.URL;

import java.util.HashSet;
import java.util.Hashtable;


/**
 * A set of utility methods for dealing with phone things
 *
 * @author  Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class PhoneUtils {


    /**  */
    private static String numverifyKey;

    /**  */
    private static String TWILIO_ACCOUNT_SID;

    /**  */
    private static String TWILIO_AUTH_TOKEN;

    /**  */
    private static String TWILIO_PHONE;


    /**
     *  @return _more_
     */
    public static boolean initTwilio() {
        if (TWILIO_ACCOUNT_SID == null) {
            TWILIO_ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
        }
        if (TWILIO_AUTH_TOKEN == null) {
            TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
        }
        if (TWILIO_PHONE == null) {
            TWILIO_PHONE = System.getenv("TWILIO_PHONE");
        }
        if (TWILIO_ACCOUNT_SID == null) {
            return false;
        }
        if (TWILIO_AUTH_TOKEN == null) {
            return false;
        }
        if (TWILIO_PHONE == null) {
            return false;
        }

        return true;
    }

    /**  */
    private static Hashtable<String, HashSet> campaigns =
        new Hashtable<String, HashSet>();

    /**
     *
     * @param phone _more_
     * @param msg _more_
     * @param ignoreInvalidNumbers _more_
     * @param campaign _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean sendSMS(String phone, String msg,
                                  boolean ignoreInvalidNumbers,
                                  String campaign)
            throws Exception {
        if ( !initTwilio()) {
            throw new IllegalArgumentException(
                "Need to set the environment variables: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_PHONE");
        }
        phone = cleanPhone(phone);
        if ( !isValidPhone(phone)) {
            if ( !ignoreInvalidNumbers) {
                throw new IllegalArgumentException("Invalid phone number:"
                        + phone);
            }
            System.err.println("Invalid phone number:" + phone);

            return false;
        }
        File    f    = null;

        HashSet seen = null;
        if (campaign != null) {
            f    = new File(campaign + ".sent.txt");
            seen = campaigns.get(campaign);
            if (seen == null) {
                seen = new HashSet();
                campaigns.put(campaign, seen);
                if (f.exists()) {
                    FileInputStream   fis = new FileInputStream(f);
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader    fr  = new BufferedReader(isr);
                    String            line;
                    while ((line = fr.readLine()) != null) {
                        seen.add(line);
                    }
                    fis.close();
                }
            }
            if (seen.contains(phone)) {
                System.err.println("Already sent:" + phone);

                return true;
            }
        }


        String url = "https://api.twilio.com/2010-04-01/Accounts/"
                     + TWILIO_ACCOUNT_SID + "/Messages.json";
        String body = "From=" + URLEncoder.encode(TWILIO_PHONE, "UTF-8")
                      + "&" + "To=" + URLEncoder.encode(phone, "UTF-8") + "&"
                      + "Body=" + URLEncoder.encode(msg, "UTF-8");
        String     result  = doPostTwilio(url, body);
        JSONObject obj     = new JSONObject(result);
        Object     _status = obj.opt("status");
        if (_status == null) {
            System.err.println("no status in response:" + result);

            return false;
        }
        String status = _status.toString();
        if (status.equals("400")) {
            System.err.println("error:" + obj.opt("message"));

            return false;
        }

        if ( !status.equals("queued")) {
            System.err.println("error: unknown status:" + status
                               + " message:" + obj.opt("message"));

            return false;
        }
        System.err.println("sent:" + phone);
        if (campaign != null) {
            seen.add(phone);
            FileWriter fw = new FileWriter(f, true);
            fw.write(phone + "\n");
            fw.close();
        }

        return true;
    }

    /**
     *
     * @param url _more_
     * @param args _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private static String doPostTwilio(String url, String args)
            throws Exception {
        URL               myurl    = new URL(url);
        HttpURLConnection huc = (HttpURLConnection) myurl.openConnection();
        String            auth = TWILIO_ACCOUNT_SID + ":" + TWILIO_AUTH_TOKEN;
        String            encoding = Utils.encodeBase64(auth);
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
            //            System.err.println("TwilioApiHandler error:" + huc.getResponseMessage());
            //            System.err.println(result);
            if (true) {
                return result;
            }

            throw new IllegalArgumentException(result);
        }
    }


    /**
     *
     * @param phone _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean isPhoneMobile(String phone) throws Exception {
        Hashtable result = getPhoneInfo(phone);
        if (result == null) {
            return false;
        }

        return result.get("line_type").equals("mobile");
    }

    /**
     *
     * @param phone _more_
     *  @return _more_
     */
    public static boolean isValidPhone(String phone) {
        phone = cleanPhone(phone);
        if (phone.length() != 12) {
            return false;
        }
        if (phone.matches("[^\\+0-9]")) {
            return false;
        }

        return true;
    }


    /**
     *
     * @param phone _more_
     *  @return _more_
     */
    public static String cleanPhone(String phone) {
        phone = phone.replaceAll("-", "").replaceAll("-", "");
        if ( !phone.startsWith("+")) {
            if ( !phone.startsWith("1")) {
                phone = "1" + phone;
            }
            phone = "+" + phone;
        }

        return phone;
    }

    /**
     *
     * @param phone _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static Hashtable getPhoneInfo(String phone) throws Exception {
        if (numverifyKey == null) {
            numverifyKey = System.getenv("NUMVERIFY_API_KEY");
            if (numverifyKey == null) {
                throw new IllegalArgumentException(
                    "No NUMVERIFY_API_KEY defined");
            }
        }
        phone = cleanPhone(phone);
        String url = HtmlUtils.url("http://apilayer.net/api/validate",
                                   "access_key", numverifyKey, "number",
                                   phone, "country_code", "", "format", "1");

        String result = IO.readContents(url, PhoneUtils.class);
        //      System.err.println(url);
        //      System.err.println(result);
        JSONObject obj = new JSONObject(result);
        if ( !obj.isNull("error")) {
            obj = obj.getJSONObject("error");

            throw new IllegalArgumentException("Error fetching url:" + url
                    + "\nerror:" + obj.opt("info"));
        }
        if ( !obj.getBoolean("valid")) {
            return null;
        }

        return Utils.makeHashtable("country_code",
                                   obj.getString("country_code"), "location",
                                   obj.getString("location"), "line_type",
                                   obj.getString("line_type"));

    }


    /**
     *
     * @param msg _more_
     */
    public static void usage(String msg) {
        if (msg != null) {
            System.err.println(msg);
        }
        System.err.println(
            "usage: PhoneUtils -campaign testcampaign -numbers numbers.txt -message \"\" <or -message file:message.txt>");
        Utils.exitTest(1);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        System.err.println(isPhoneMobile("3038982413"));
        System.err.println(isPhoneMobile("3035437510"));
        if (true) {
            return;
        }


        String campaign = "testcampaign";
        String message  = null;
        String numbers  = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-help")) {
                usage(null);
            }
            if (arg.equals("-campaign")) {
                campaign = args[++i];
                continue;
            }
            if (arg.equals("-numbers")) {
                numbers = args[++i];
                continue;
            }
            if (arg.equals("-message")) {
                message = args[++i];
                if (message.startsWith("file:")) {
                    message = IO.readContents(message.substring(5));
                }
                continue;
            }
        }

        if (numbers == null) {
            usage("No numbers file provided");
        }
        if (message == null) {
            usage("No message provided");
        }
        for (String line :
                Utils.split(IO.readContents(numbers), "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            if ( !sendSMS(line, message, false, campaign)) {
                break;
            }
        }
        Utils.exitTest(0);
    }




}
