/*
* Copyright (c) 2008-2018 Geode Systems LLC
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


import org.ramadda.repository.Entry;

import ucar.unidata.util.Misc;

import java.io.File;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Class description
 *
 *
 */
public class ServiceInput {

    /** the process directory for this input */
    private File processDir;

    /** The operands for this input */
    private List<ServiceOperand> operands;

    /** Properties */
    private Hashtable properties = new Hashtable();

    /** publish? */
    private boolean publish = false;

    /** for display */
    private boolean forDisplay = false;

    /** parameters */
    private List<String[]> params = new ArrayList<String[]>();

    /** source service */
    private Service sourceService;

    /** seen files */
    private HashSet<File> seenFiles = new HashSet<File>();


    /**
     * Create a new Service Input
     */
    public ServiceInput() {
        this.operands = new ArrayList<ServiceOperand>();
    }

    /**
     * Create a service input with an entry
     *
     * @param entry  the entry
     */
    public ServiceInput(Entry entry) {
        this(new ServiceOperand(entry));
    }



    /**
     * Create a service input
     *
     * @param operand  one operand
     */
    public ServiceInput(ServiceOperand operand) {
        this(null, Misc.newList(operand));
    }

    /**
     * Create a service input in the processing directory with an entry
     *
     * @param processDir  the directory for output
     * @param entry       an entry
     */
    public ServiceInput(File processDir, Entry entry) {
        this(processDir, Misc.newList(new ServiceOperand(entry)));
    }

    /**
     * Create a service input in the processing directory with a list of entry's
     *
     * @param processDir  the directory for output
     * @param entries     list of entries
     * @param dummy       dummy variable
     */
    public ServiceInput(File processDir, List<Entry> entries, boolean dummy) {
        this(processDir, ServiceOperand.makeOperands(entries));
    }



    /**
     * Create a service input
     *
     * @param processDir  the directory for output
     * @param operand     one operand
     */
    public ServiceInput(File processDir, ServiceOperand operand) {
        this(processDir, Misc.newList(operand));
    }

    /**
     * Create a service input
     *
     * @param operands  the operands for this service
     */
    public ServiceInput(List<ServiceOperand> operands) {
        this(null, operands);
    }

    /**
     * _more_
     *
     * @param dir _more_
     */
    public ServiceInput(File dir) {
        this(dir, (List<ServiceOperand>) null);
    }


    /**
     * Create a ServiceInput from a list of operands
     *
     *
     * @param dir process directory
     * @param operands the operands
     */
    public ServiceInput(File dir, List<ServiceOperand> operands) {
        this.processDir = dir;
        this.operands   = (operands != null)
                          ? operands
                          : new ArrayList<ServiceOperand>();
        if (processDir != null) {
            for (File f : processDir.listFiles()) {
                seenFiles.add(f);
            }
        }
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        return getProperty(key, null);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key, Object dflt) {
        Object value = properties.get(key);
        if (value == null) {
            return dflt;
        }

        return value;
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param prefix _more_
     * @param value _more_
     */
    public void addParam(String key, String prefix, String value) {
        params.add(new String[] { key, value });
        if ((prefix != null) && key.startsWith(prefix)) {
            //            params.add(new String[] { key.substring(prefix.length()+1), value });
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String[]> getParams() {
        return params;
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void addSeenFile(File f) {
        seenFiles.add(f);
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public boolean haveSeenFile(File f) {
        return seenFiles.contains(f);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<Entry>();
        if (operands == null) {
            return entries;
        }
        for (ServiceOperand op : operands) {
            entries.addAll(op.getEntries());
        }

        return entries;
    }


    /**
     * _more_
     *
     * @param entries _more_
     */
    public void setEntries(List<Entry> entries) {
        operands = ServiceOperand.makeOperands(entries);
    }


    /**
     * Get the operands
     *
     * @return the list of operands
     */
    public List<ServiceOperand> getOperands() {
        return operands;
    }

    /**
     * Set the operands
     *
     * @param the list of operands
     *
     * @param operands _more_
     */
    public void setOperands(List<ServiceOperand> operands) {
        this.operands = operands;
    }

    /**
     * Get the process directory
     *
     * @return  the process directory
     */
    public File getProcessDir() {
        return processDir;
    }


    /**
     *  Does this input have any operands?
     *  return true if it has operands
     *
     * @return _more_
     */
    public boolean hasOperands() {
        return (operands != null) && !operands.isEmpty();
    }

    /**
     * Create a ServiceInput from a ServiceOutput
     * @param output  the output
     * @return a new ServiceInput
     */
    public ServiceInput makeInput(ServiceOutput output) {
        ServiceInput input = new ServiceInput(
                                 this.getProcessDir(),
                                 new ArrayList<ServiceOperand>(
                                     output.getOperands()));

        input.setPublish(this.getPublish());
        input.setForDisplay(this.getForDisplay());
        input.params     = this.params;
        input.properties = this.properties;
        //Keep the same reference to the list of seen files
        input.seenFiles = this.seenFiles;

        return input;
    }


    /**
     * Set the Publish property.
     *
     * @param value The new value for Publish
     */
    public void setPublish(boolean value) {
        publish = value;
    }

    /**
     * Get the Publish property.
     *
     * @return The Publish
     */
    public boolean getPublish() {
        return publish;
    }


    /**
     * Set the ForDisplay property.
     *
     * @param value The new value for ForDisplay
     */
    public void setForDisplay(boolean value) {
        forDisplay = value;
    }

    /**
     * Get the ForDisplay property.
     *
     * @return The ForDisplay
     */
    public boolean getForDisplay() {
        return forDisplay;
    }

    /**
     *  Set the SourceService property.
     *
     *  @param value The new value for SourceService
     */
    public void setSourceService(Service value) {
        sourceService = value;
    }

    /**
     *  Get the SourceService property.
     *
     *  @return The SourceService
     */
    public Service getSourceService() {
        return sourceService;
    }





}
