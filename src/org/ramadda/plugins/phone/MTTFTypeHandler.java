/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.phone;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class MTTFTypeHandler extends GenericTypeHandler {

    /** _more_ */
    private static int IDX_FIRST = 0;

    /** _more_ */
    public static final int IDX_ENABLED = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_RECURRENCE = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_RECURRENCE_VALUE = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_SUBJECT = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_TO_PHONE = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_FROM_EMAIL = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_TO_EMAIL = IDX_FIRST++;


    /** _more_ */
    public static final int IDX_MESSAGE = IDX_FIRST++;

    /** _more_ */
    public static final int IDX_STATUS = IDX_FIRST++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MTTFTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /** _more_ */
    private static Boolean twilioEnabled;


    /**
     * _more_
     *
     * @param request _more_
     * @param column _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry parentEntry, Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)
            throws Exception {
        if (twilioEnabled == null) {
            TwilioApiHandler twilio =
                (TwilioApiHandler) getRepository().getApiManager()
                    .getApiHandler("twilio");
            if (twilio != null) {
                if ( !twilio.sendingEnabled()) {
                    twilio = null;
                }
            }
            twilioEnabled =  Boolean.valueOf(twilio != null);
        }

        if ( !getMailManager().isEmailEnabled()
                && !twilioEnabled.booleanValue()) {
            if (column.getName().equals("enabled")) {
                formBuffer.append(formEntryTop(request, "",
                        "No email or SMS available"));
            }

            return;
        }

        if (column.getName().equals("to_phone")) {
            if ( !twilioEnabled.booleanValue()) {
                return;
            }
        } else if (column.getName().equals("from_email")) {
            if ( !getMailManager().isEmailEnabled()) {
                return;
            }
        } else if (column.getName().equals("to_email")) {
            if ( !getMailManager().isEmailEnabled()) {
                return;
            }
        }
        super.addColumnToEntryForm(request, column, formBuffer, parentEntry, entry,
                                   values, state, formInfo,
                                   sourceTypeHandler);
    }


}
