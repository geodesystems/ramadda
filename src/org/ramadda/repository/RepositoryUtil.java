/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import java.net.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.text.SimpleDateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.List;
import java.util.TimeZone;
import java.util.TimeZone;

/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RepositoryUtil {

    /**
     *  The versions are set in the top level build.properties file
     */

    private static String VERSION = "1.0";

    /**  */
    private static String VERSION_FULL = VERSION + ".0";

    /**  */
    private static int requestCnt = 0;

    private static String HTDOCS_VERSION = "htdocs_v"
                                           + VERSION_FULL.replaceAll("\\.",
                                               "_");

    private static String HTDOCS_VERSION_SLASH = "/" + HTDOCS_VERSION;

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
     *
     * @param major _more_
     * @param minor _more_
     * @param patch _more_
     */
    protected static void setVersion(String major, String minor,
                                     String patch) {
        VERSION              = major + "." + minor;
        VERSION_FULL         = VERSION + "." + patch;
        HTDOCS_VERSION = "htdocs_v" + VERSION_FULL.replaceAll("\\.", "_");
        HTDOCS_VERSION_SLASH = "/" + HTDOCS_VERSION;
    }

    /**
      * @return _more_
     */
    public static String getMajorMinorVersion() {
        return VERSION;
    }

    /**
      * @return _more_
     */
    public static String getVersion() {
        return VERSION_FULL;
    }

    public static String getHtdocsVersion() {
	return HTDOCS_VERSION +"_"+new Date().getTime();
	//        return HTDOCS_VERSION;
    }

    public static String getHtdocsVersionSlash() {
	return HTDOCS_VERSION_SLASH +"_"+new Date().getTime();
	//        return HTDOCS_VERSION_SLASH;
    }

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

    public static void main(String[] args) throws Exception {}

}
