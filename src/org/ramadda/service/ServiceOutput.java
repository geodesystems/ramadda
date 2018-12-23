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

package org.ramadda.service;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;




import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.util.regex.*;
import java.util.zip.*;


/**
 * Class description
 *
 *
 * @version        $version$, Wed, Sep 17, '14
 * @author         Enter your name here...
 */
public class ServiceOutput {

    /** _more_ */
    private boolean ok = true;

    /** _more_ */
    private boolean resultsShownAsText = false;

    /** _more_ */
    private StringBuilder results = new StringBuilder();

    /** _more_ */
    private List<Entry> entries = new ArrayList<Entry>();



    /** empty output id */
    public int SERVICE_OUTPUT_EMPTY = 0;

    /** operand output id */
    public int SERVICE_OUTPUT_OPERAND = 1;


    /**
     * _more_
     */
    public ServiceOutput() {}

    /**
     * _more_
     *
     * @param ok _more_
     * @param message _more_
     */
    public ServiceOutput(boolean ok, String message) {
        this.ok = ok;
        this.results.append(message);
    }

    /**
     * Create a ServiceOutput from the file
     *
     *
     * @param entry _more_
     */
    public ServiceOutput(Entry entry) {
        addEntry(entry);
    }

    /**
     * Create the ServiceOutput from data operand
     *
     * @param operand the operand
     */
    public ServiceOutput(ServiceOperand operand) {
        entries.addAll(operand.getEntries());
    }



    /**
     * _more_
     *
     * @param operands _more_
     */
    public ServiceOutput(List<ServiceOperand> operands) {
        for (ServiceOperand op : operands) {
            entries.addAll(op.getEntries());
        }
    }


    /*
    public ServiceOutput(List<Entry> entries) {
        this.entries.addAll(entries);
    }
    */


    /**
     * _more_
     *
     * @param entry _more_
     */
    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Entry> getEntries() {
        return entries;
    }


    /**
     * Get the operands
     *
     * @return the operands or an empty list
     */
    public List<ServiceOperand> getOperands() {
        List<ServiceOperand> operands = new ArrayList<ServiceOperand>();
        for (Entry entry : entries) {
            operands.add(new ServiceOperand(entry.getName(), entry));
        }

        return operands;
    }

    /**
     * Get the ServiceOutput type
     *
     * @return
     */
    public int getServiceOutputType() {
        if (entries.isEmpty()) {
            return SERVICE_OUTPUT_EMPTY;
        }

        return SERVICE_OUTPUT_OPERAND;
    }

    /**
     * Check if this has output
     *
     * @return  true if output type is not EMPTY
     */
    public boolean hasOutput() {
        return getServiceOutputType() != SERVICE_OUTPUT_EMPTY;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * _more_
     *
     * @param b _more_
     */
    public void setOk(boolean b) {
        this.ok = b;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void append(String s) {
        results.append(s);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getResults() {
        return results.toString();
    }



    /**
     * Set the ResultsShownAsText property.
     *
     * @param value The new value for ResultsShownAsText
     */
    public void setResultsShownAsText(boolean value) {
        resultsShownAsText = value;
    }

    /**
     * Get the ResultsShownAsText property.
     *
     * @return The ResultsShownAsText
     */
    public boolean getResultsShownAsText() {
        return resultsShownAsText;
    }


}
