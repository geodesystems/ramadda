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
                                     Appendable formBuffer, Entry entry,
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
            twilioEnabled = new Boolean(twilio != null);
        }

        if ( !getRepository().getAdmin().isEmailCapable()
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
            if ( !getRepository().getAdmin().isEmailCapable()) {
                return;
            }
        } else if (column.getName().equals("to_email")) {
            if ( !getRepository().getAdmin().isEmailCapable()) {
                return;
            }
        }
        super.addColumnToEntryForm(request, column, formBuffer, entry,
                                   values, state, formInfo,
                                   sourceTypeHandler);
    }


}
