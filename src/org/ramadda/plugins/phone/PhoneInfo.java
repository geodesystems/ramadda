/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.phone;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;



import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;



import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;



/**
 */
public class PhoneInfo {

    /** _more_ */
    public static final String TYPE_SMS = "sms";

    /** _more_ */
    public static final String TYPE_VOICE = "voice";

    /** _more_ */
    private String type = TYPE_SMS;

    /** _more_ */
    private String fromPhone;

    /** _more_ */
    private String toPhone;

    /** _more_ */
    private String passCode;

    /** _more_ */
    private String message;

    /** _more_ */
    private String recordingUrl;

    /** _more_ */
    private String fromZip;

    /** _more_ */
    private String transcription = "";


    /**
     *
     * @param type _more_
     * @param fromPhone _more_
     * @param toPhone _more_
     * @param passCode _more_
     *
     * @throws Exception _more_
     */
    public PhoneInfo(String type, String fromPhone, String toPhone,
                     String passCode)
            throws Exception {
        this.type      = type;
        this.fromPhone = fromPhone;
        this.toPhone   = toPhone;
        this.passCode  = passCode;
    }

    /**
     *  Set the FromPhone property.
     *
     *  @param value The new value for FromPhone
     */
    public void setFromPhone(String value) {
        fromPhone = value;
    }

    /**
     *  Get the FromPhone property.
     *
     *  @return The FromPhone
     */
    public String getFromPhone() {
        return fromPhone;
    }

    /**
     *  Set the ToPhone property.
     *
     *  @param value The new value for ToPhone
     */
    public void setToPhone(String value) {
        toPhone = value;
    }

    /**
     *  Get the ToPhone property.
     *
     *  @return The ToPhone
     */
    public String getToPhone() {
        return toPhone;
    }

    /**
     *  Set the PassCode property.
     *
     *  @param value The new value for PassCode
     */
    public void setPassCode(String value) {
        passCode = value;
    }

    /**
     *  Get the PassCode property.
     *
     *  @return The PassCode
     */
    public String getPassCode() {
        return passCode;
    }

    /**
     *  Set the Message property.
     *
     *  @param value The new value for Message
     */
    public void setMessage(String value) {
        message = value;
    }

    /**
     *  Get the Message property.
     *
     *  @return The Message
     */
    public String getMessage() {
        return message;
    }

    /**
     *  Set the RecordingUrl property.
     *
     *  @param value The new value for RecordingUrl
     */
    public void setRecordingUrl(String value) {
        recordingUrl = value;
    }

    /**
     *  Get the RecordingUrl property.
     *
     *  @return The RecordingUrl
     */
    public String getRecordingUrl() {
        return recordingUrl;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSms() {
        return type.equals(TYPE_SMS);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isVoice() {
        return type.equals(TYPE_VOICE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "type:" + type + " from:" + fromPhone + " to:" + toPhone
               + (isSms()
                  ? " " + message
                  : "");
    }


    /**
     *  Set the FromZip property.
     *
     *  @param value The new value for FromZip
     */
    public void setFromZip(String value) {
        fromZip = value;
    }

    /**
     *  Get the FromZip property.
     *
     *  @return The FromZip
     */
    public String getFromZip() {
        return fromZip;
    }


    /**
     * Set the Transcription property.
     *
     * @param value The new value for Transcription
     */
    public void setTranscription(String value) {
        transcription = value;
    }

    /**
     * Get the Transcription property.
     *
     * @return The Transcription
     */
    public String getTranscription() {
        return transcription;
    }


}
