/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.json.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.io.*;

import java.net.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


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
    private static String numlookupapiKey;

    /**  */
    private static String TWILIO_ACCOUNT_SID;

    /**  */
    private static String TWILIO_AUTH_TOKEN;

    /**  */
    private static String TWILIO_PHONE;


    /**  */
    private static boolean haveInited = false;

    /**
     */
    public static void initKeys() {
        if (haveInited) {
            return;
        }
        haveInited = true;
        //      numverifyKey = System.getenv("NUMVERIFY_API_KEY");
        numlookupapiKey = System.getenv("NUMLOOKUPAPI_API_KEY");
    }


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

    /**  */
    public static final int SMS_CODE_SENT = 0;

    /**  */
    public static final int SMS_CODE_ALREADYSENT = 1;

    /**  */
    public static final int SMS_CODE_BADPHONE = 2;

    /**  */
    public static final int SMS_CODE_ERROR = 3;

    /**  */
    public static final int SMS_CODE_UNSUBSCRIBED = 4;

    /**
     *
     * @param campaign _more_
      * @return _more_
     */
    private static File getCampaignFile(String campaign) {
        return new File(campaign + ".sent.txt");
    }

    /**
     *
     * @param campaign _more_
     * @param phone _more_
     * @param status _more_
     *
     * @throws Exception _more_
     */
    private static void writeCampaignFile(String campaign, String phone,
                                          String status)
            throws Exception {
        if (campaign == null) {
            return;
        }
        FileWriter fw = new FileWriter(getCampaignFile(campaign), true);
        fw.write(phone + ":" + status + "\n");
        fw.close();
    }

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
    public static int sendSMS(String phone, String msg,
                              boolean ignoreInvalidNumbers, String campaign)
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

            return SMS_CODE_BADPHONE;
        }

        HashSet seen = null;
        if (campaign != null) {
            File f = getCampaignFile(campaign);
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
                        line = line.trim();
                        List<String> toks = Utils.splitUpTo(line, ":", 2);
                        seen.add(toks.get(0));
                    }
                    fis.close();
                }
            }
            if (seen.contains(phone)) {
                //                System.err.println("Already sent:" + phone);
                return SMS_CODE_ALREADYSENT;
            }
        }


        //      https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json


        String url = "https://api.twilio.com/2010-04-01/Accounts/"
                     + TWILIO_ACCOUNT_SID + "/Messages.json";
        String body = "From=" + URLEncoder.encode(TWILIO_PHONE, "UTF-8")
                      + "&" + "To=" + URLEncoder.encode(phone, "UTF-8") + "&"
                      + "Body=" + URLEncoder.encode(msg, "UTF-8");
        int MAX_TRIES = 20;
        int tries     = 0;
        int deltaTime = 100;
        while (tries++ <= MAX_TRIES) {
            String     result  = doPostTwilio(url, body);
            JSONObject obj     = new JSONObject(result);
            String status = obj.optString("status");
            if (status == null) {
                System.err.println("no status in response:" + result);
                return SMS_CODE_ERROR;
            }
            if (status.equals("queued") || status.equals("200")) {
		if (seen != null) {
		    seen.add(phone);
		}
		writeCampaignFile(campaign, phone, "sent");
		return SMS_CODE_SENT;
	    }

            int code = obj.optInt("code", -1);
	    String    message = obj.optString("message",result);

            if (status.equals("429")) {
                //Rate limited so we do an exponential backoff
                //2^1*deltaTime+rand,2^2*deltaTime+rand,2^3*deltaTime+rand,
                int sleep = (int) (Math.pow(2, tries) * deltaTime
                                   + deltaTime * Math.random());
                sleep = Math.min(sleep, 10000);
                System.err.println("Calling too fast. Sleeping for " + sleep
                                   + " ms");
                Misc.sleep(sleep);
                continue;
            }

            if (code == 21610) {
                writeCampaignFile(campaign, phone, "unsubscribed");
                return SMS_CODE_UNSUBSCRIBED;
            }
            if (status.equals("400")) {
                if (code == 21211) {
                    //Write the bad phone
                    writeCampaignFile(campaign, phone, "failed");
                    return SMS_CODE_BADPHONE;
                }
                System.err.println("error:" + message);
                return SMS_CODE_ERROR;
            }
	    System.err.println("error: unknown status:" + status  + " message:" + message);
	    return SMS_CODE_ERROR;
	}
        System.err.println("Too many tries");
        return SMS_CODE_ERROR;

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

    /**  */
    private static Hashtable<String, Boolean> isMobile;

    /**  */
    private static List<String> numbers;

    /**
     *
     * @param phone _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean isPhoneMobile(String phone) throws Exception {
        File cacheFile = IO.getCacheFile("ismobile.txt");
        if (isMobile == null) {
            isMobile = new Hashtable<String, Boolean>();
            numbers  = new ArrayList<String>();
            if (cacheFile.exists()) {
                for (String num :
                        Utils.split(IO.readContents(cacheFile), "\n", true,
                                    true)) {
                    List<String> toks = Utils.splitUpTo(num, ":", 2);
                    numbers.add(num);
                    num = toks.get(0);
                    boolean good = toks.get(1).trim().equals("true");
                    isMobile.put(num, new Boolean(good));
                }
            }
        }
        Boolean b = isMobile.get(phone);
        if (b != null) {
	    //            System.err.println("cached:" + phone);
            return b.booleanValue();
        }
        PhoneInfo result = getPhoneInfo(phone);
        if (result == null) {
            b = new Boolean(false);
        } else {
            b = new Boolean(result.lineType.equals("mobile"));
        }
        isMobile.put(phone, b);
        numbers.add(phone + ":" + b);
        IOUtil.writeFile(cacheFile, Utils.join(numbers, "\n"));

        return b.booleanValue();
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
        phone = phone.replaceAll("[^0-9]+", "");
        //        phone = phone.replaceAll("-", "").replaceAll("-", "").replaceAll("\\s","").replaceAll("\\(","").replaceAll("\\)","").replaceAll("\\.","");
        //Assume US
        if ( !phone.startsWith("+")) {
            if ( !phone.startsWith("1")) {
                phone = "1" + phone;
            }
            phone = "+" + phone;
        }

        return phone;
    }

    public static String formatPhone(String phone) {
	if(phone==null) return "";
        phone = phone.replaceAll("[^0-9]+", "");
	System.err.println("PHONE:" + phone);
	if(phone.startsWith("1")) phone = phone.substring(1);
	phone = phone.replaceAll("^(\\d\\d\\d)(\\d\\d\\d)(\\d\\d\\d\\d)$", "$1-$2-$3");
        return phone;
    }



    private static boolean printedMessage = false;


    /**
     *
     * @param phone _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static PhoneInfo getPhoneInfo(String phone) throws Exception {
        initKeys();
        if ((numverifyKey == null) && (numlookupapiKey == null)) {
            throw new IllegalArgumentException(
                "No NUMVERIFY_API_KEY or NUMLOOKUPAPI_API_KEY defined");
        }
        phone = cleanPhone(phone);
        if (phone.length() != 12) {
            System.err.println("BAD PHONE:" + phone);
            return null;
        }
        if (numverifyKey != null) {
	    if(!printedMessage) {
		printedMessage = true;
		System.err.println("Using numverify");
	    }
            String url = HtmlUtils.url("http://apilayer.net/api/validate",
                                       "access_key", numverifyKey, "number",
                                       phone, "country_code", "", "format",
                                       "1");
            String result = IO.readContents(url, PhoneUtils.class);
            //      System.err.println(url);
            //      System.err.println(result);
            JSONObject obj = new JSONObject(result);
            if ( !obj.isNull("error")) {
                obj = obj.getJSONObject("error");

                throw new IllegalArgumentException("Error fetching url:"
                        + url + "\nerror:" + obj.opt("info"));
            }
            if ( !obj.getBoolean("valid")) {
                return null;
            }

            return new PhoneInfo(obj.getString("country_code"),
                                 obj.getString("location"),
                                 obj.getString("line_type"));
        }

	if(!printedMessage) {
	    printedMessage = true;
	    System.err.println("Using numlookup");
	}
        String url =
            HtmlUtils.url("https://api.numlookupapi.com/v1/validate/"
                          + phone, "apikey", numlookupapiKey);
        String result = IO.readContents(url, PhoneUtils.class);
        //      System.err.println(url);
        //      System.err.println(result);
        /*
{
  "valid": true,
  "number": "18004190157",
  "local_format": "8004190157",
  "international_format": "+18004190157",
  "country_prefix": "+1",
  "country_code": "US",
  "country_name": "United States of America",
  "location": "",
  "carrier": "",
  "line_type": "toll_free"
  }*/

        JSONObject obj = new JSONObject(result);
        if ( !obj.isNull("error")) {
            obj = obj.getJSONObject("error");

            throw new IllegalArgumentException("Error fetching url:" + url
                    + "\nerror:" + obj.opt("info"));
        }
        if ( !obj.getBoolean("valid")) {
            return null;
        }

        return new PhoneInfo(obj.getString("country_code"),
                             obj.getString("location"),
                             obj.getString("line_type"));


    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Oct 12, '22
     * @author         Enter your name here...    
     */
    private static class PhoneInfo {

        /**  */
        String countryCode;

        /**  */
        String location;

        /**  */
        String lineType;

        /**
         
         *
         * @param countryCode _more_
         * @param location _more_
         * @param lineType _more_
         */
        PhoneInfo(String countryCode, String location, String lineType) {
            this.countryCode = countryCode;
            this.location    = location;
            this.lineType    = lineType;
        }
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
        for (int i = 0; i < 10; i++) {
            System.err.println(isPhoneMobile("3038982413"));
            System.err.println(isPhoneMobile("3035437510"));
        }
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
            if (sendSMS(line, message, false, campaign) == SMS_CODE_ERROR) {
                break;
            }
        }
        Utils.exitTest(0);
    }




}
