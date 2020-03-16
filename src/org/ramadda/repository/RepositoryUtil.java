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

package org.ramadda.repository;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;
import java.io.*;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RepositoryUtil {

    //Make sure to change the fields in the top-level build.properties

    /** _more_ */
    private static final double MAJOR_VERSION = 3.0;

    /** _more_ */
    private static final int MINOR_VERSION = 45;


    //When we make any real change to the css or javascript change this version
    //so the browsers will pick up the new resource
    //The imports.html header has a ${htdocs_version} macro in it
    //that gets replaced with  this. Repository checks incoming paths and strips this off

    /** _more_ */
    private static final String HTDOCS_VERSION =
        "htdocs_v" + Double.toString(MAJOR_VERSION).replace(".", "_") + "_"
        + MINOR_VERSION;


    /** _more_ */
    private static final String HTDOCS_VERSION_SLASH = "/" + HTDOCS_VERSION;


    /** _more_ */
    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");



    /** timezone */
    public static final TimeZone TIMEZONE_DEFAULT =
        TimeZone.getTimeZone("UTC");


    /** the file separator id */
    public static final String FILE_SEPARATOR = "_file_";

    /** The regular expression that matches the entry id */
    public static final String ENTRY_ID_REGEX =
        "[a-f|0-9]{8}-([a-f|0-9]{4}-){3}[a-f|0-9]{12}_";


    /**
     * _more_
     *
     * @return _more_
     */
    public static String getHtdocsVersion() {
        return HTDOCS_VERSION;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String getHtdocsVersionSlash() {
        return HTDOCS_VERSION_SLASH;
    }





    /**
     * _more_
     *
     * @param string _more_
     *
     * @return _more_
     */
    public static String hashString(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(string.getBytes("UTF-8"));
            byte[] bytes  = md.digest();
            String s      = new String(bytes);
            String result = Utils.encodeBase64Bytes(bytes);

            //            System.err.println("Hash input string:" + string  +":");
            //            System.err.println("Hash result:" + s  +":");
            //            System.err.println("Hash base64:" + result  +":");
            return result.trim();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }








    /**
     * Make a date format from the format string
     *
     * @param formatString  the format string
     *
     * @return  the date formatter
     */
    public static SimpleDateFormat makeDateFormat(String formatString) {
        return makeDateFormat(formatString, null);
    }

    /**
     * _more_
     *
     * @param formatString _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String formatString,
            TimeZone timezone) {
        if (timezone == null) {
            timezone = TIMEZONE_DEFAULT;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.setTimeZone(timezone);
        dateFormat.applyPattern(formatString);

        return dateFormat;
    }


    /**
     * This will prune out any leading &lt;unique id&gt;_file_&lt;actual file name&gt;
     *
     * @param fileName the filename
     *
     * @return  the pruned filename
     */
    public static String getFileTail(String fileName) {
        return Utils.getFileTail(fileName);
    }


    /**
     * MissingEntry Exception
     *
     * @author RAMADDA Development Team
     */
    public static class MissingEntryException extends Exception {

        /**
         * Create an exception with the message
         *
         * @param msg  the message
         */
        public MissingEntryException(String msg) {
            super(msg);
        }
    }


    /**
     * Make a header from the String
     *
     * @param h  the header text
     *
     * @return  the header
     */
    public static String header(String h) {
        return HtmlUtils.div(
            h, HtmlUtils.cssClass(
                "ramadda-page-heading ramadda-page-heading-left"));
    }









    /**
     * Make a hash of the plain text password
     *
     * @param password  the password
     *
     * @return  the hashed pw
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(password.getBytes("UTF-8"));
            byte[] bytes  = md.digest();
            String result = Utils.encodeBase64Bytes(bytes);

            return result.trim();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }


    /**
     * This is a routine created by Matias Bonet to handle pre-existing passwords that
     * were hashed via md5
     *
     * @param password The password
     *
     * @return hashed password
     */
    public static String hashPasswordForOldMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes("UTF-8"));
            byte         messageDigest[] = md.digest();
            StringBuffer hexString       = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            //            System.out.println(hexString.toString());
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {}



}
