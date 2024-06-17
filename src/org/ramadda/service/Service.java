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
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.Enumeration;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.util.regex.*;
import java.util.zip.*;


/**
 *
 */
@SuppressWarnings("unchecked")
public class Service extends RepositoryManager {

    private static final String MACRO_OUTPUTDIR = "${outputdir}";

    /** _more_ */
    public static boolean debug = false;

    /** _more_ */
    private static ServiceUtil dummyToForceCompile;

    /** _more_ */
    public static final String ARG_SERVICEFORM = "serviceform";

    /** _more_ */
    public static final String TAG_ARG = "arg";

    /** _more_ */
    public static final String TAG_PARAMS = "params";

    /** _more_ */
    public static final String TAG_PARAM = "param";

    /** _more_ */
    public static final String ATTR_COMMAND = "command";

    /** _more_ */
    public static final String ATTR_CLEANUP = "cleanup";

    /** _more_ */
    public static final String ATTR_LINK = "link";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_MAXFILESIZE = "maxFileSize";

    /** _more_ */
    public static final String ATTR_PRIMARY = "primary";

    /** _more_ */
    public static final String ATTR_ENTRY_TYPE = "entryType";

    /** _more_ */
    public static final String ATTR_TARGET = "target";

    /** _more_ */
    public static final String ATTR_TARGET_TYPE = "target.type";

    /** _more_ */
    public static final String ATTR_ENTRY_PATTERN = "entryPattern";

    /** _more_ */
    public static final String ATTR_ICON = "icon";

    /** _more_ */
    public static final String TAG_OUTPUT = "output";

    /** _more_ */
    public static final String TAG_INPUT = "input";

    /** _more_ */
    public static final String TAG_SERVICE = "service";

    /** _more_ */
    public static final String TAG_SERVICES = "services";


    /** _more_ */
    public static final String ATTR_CATEGORY = "category";

    /** _more_ */
    public static final String ATTR_VALUES = "values";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_HELP = "help";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_SERIAL = "serial";


    /** _more_ */
    public static final String ATTR_FILE = "file";

    /** _more_ */
    public static final String ATTR_SERVICE = "service";

    /** _more_ */
    public static final String ATTR_PATHPROPERTY = "pathProperty";

    /** _more_ */
    public static final String ARG_DELIMITER = ".";



    /** _more_ */
    private String id;

    /** _more_ */
    private Entry serviceEntry;

    /** _more_ */
    private String icon;

    private List<String> ignore;

    /** _more_ */
    private String entryType;

    /** _more_ */
    private boolean enabled = false;

    /** _more_ */
    private double maxFileSize = -1;

    /** _more_ */
    private Boolean requiresMultipleEntries;

    /** _more_ */
    private boolean outputToStderr = false;

    /** _more_ */
    private boolean immediate = false;

    /** _more_ */
    private boolean ignoreStderr = false;

    /** _more_ */
    private String errorPattern;


    /** _more_ */
    private String target;

    /** _more_ */
    private String targetType;

    /** _more_ */
    private String namePattern;

    /** _more_ */
    private String descriptionPattern;

    /** _more_ */
    private boolean cleanup = false;

    /** _more_ */
    private String command;

    /** _more_ */
    private Object commandObject;

    /** _more_ */
    private Method commandMethod;

    /** _more_ */
    private String description;

    /** _more_ */
    private String category;

    /** _more_ */
    private String processDesc;

    /** _more_ */
    private String label;

    /** _more_ */
    private String pathProperty;

    /** _more_ */
    private Service parent;

    /** _more_ */
    private List<Service> children;

    /** _more_ */
    public boolean serial;

    /** _more_ */
    private String linkId;

    private boolean optional = false;
    
    /** _more_ */
    private Service link;

    /** _more_ */
    private List<ServiceArg> args = new ArrayList<ServiceArg>();

    /** _more_ */
    private List<ServiceArg> inputs = new ArrayList<ServiceArg>();

    /** _more_ */
    private List<OutputDefinition> outputs =
        new ArrayList<OutputDefinition>();


    /** _more_ */
    private Hashtable paramValues = new Hashtable();

    /** _more_ */
    private Element element;


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public Service(Repository repository, Element element) throws Exception {
        super(repository);
        init(null, element, null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param entry _more_
     */
    public Service(Repository repository, Entry entry) {
        this(repository, entry.getId(), entry.getName());
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     * @param label _more_
     */
    public Service(Repository repository, String id, String label) {
        super(repository);
        this.id    = id;
        this.label = label;
    }




    /**
     * _more_
     *
     * @param repository _more_
     * @param parent _more_
     * @param element _more_
     * @param index _more_
     *
     * @throws Exception _more_
     */
    public Service(Repository repository, Service parent, Element element,
                   int index)
            throws Exception {
        super(repository);
        init(parent, element, "service_" + index);
    }




    /**
     * _more_
     *
     * @param msg _more_
     */
    private static void debug(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    /**
     * _more_
     *
     *
     * @param parent _more_
     * @param element _more_
     * @param dfltId _more_
     *
     * @throws Exception _more_
     */
    private void init(Service parent, Element element, String dfltId)
            throws Exception {

        if (element == null) {
            return;
        }

        this.element = element;
        this.parent  = parent;
        id           = XmlUtil.getAttribute(element, ATTR_ID, dfltId);
        if (id == null) {
            //            id = "dummy";
        }

        entryType = XmlUtil.getAttribute(element, ATTR_ENTRY_TYPE,
                                         (String) null);


        maxFileSize = Double.parseDouble(XmlUtil.getAttributeFromTree(element,
                ATTR_MAXFILESIZE, "-1.0"));

        String ignoreString  = XmlUtil.getGrandChildText(element, "ignoreerrors",null);
	if(Utils.stringDefined(ignoreString)) {
	    ignore = Utils.split(ignoreString,"\n",true,true);
	}
        errorPattern = XmlUtil.getAttributeFromTree(element, "errorPattern",
                (String) null);



        icon = XmlUtil.getAttributeFromTree(element, ATTR_ICON,
                                            (String) null);
        outputToStderr = XmlUtil.getAttributeFromTree(element,
                "outputToStderr", outputToStderr);

        immediate = XmlUtil.getAttributeFromTree(element, "immediate", false);

        ignoreStderr = XmlUtil.getAttributeFromTree(element, "ignoreStderr",
                ignoreStderr);


        target = XmlUtil.getAttributeFromTree(element, ATTR_TARGET,
                (String) null);
        targetType = XmlUtil.getAttributeFromTree(element, ATTR_TARGET_TYPE,
                (String) null);
        namePattern = XmlUtil.getAttributeFromTree(element, "namePattern",
                (String) null);
        descriptionPattern = XmlUtil.getAttributeFromTree(element,
                "descriptionPattern", (String) null);
        if (namePattern != null) {
            namePattern = namePattern.replaceAll("\\n", "\n");
            namePattern = namePattern.replaceAll("\\r", "\r");
        }
        if (descriptionPattern != null) {
            descriptionPattern = descriptionPattern.replaceAll("\\n", "\n");
            descriptionPattern = descriptionPattern.replaceAll("\\r", "\r");
        }
        cleanup = XmlUtil.getAttributeFromTree(element, ATTR_CLEANUP, true);
        category = XmlUtil.getAttributeFromTree(element, "category",
                (String) null);
	
        optional = XmlUtil.getAttribute(element, "optional",false);

        linkId = XmlUtil.getAttribute(element, ATTR_LINK, (String) null);
        description = XmlUtil.getGrandChildText(element, ATTR_DESCRIPTION,
                XmlUtil.getGrandChildText(element, ATTR_HELP, ""));

        processDesc = XmlUtil.getGrandChildText(element,
                "process_description", "");
        label  = XmlUtil.getAttribute(element, ATTR_LABEL, (String) null);
        serial = XmlUtil.getAttribute(element, ATTR_SERIAL, true);


        NodeList nodes;

        Element  params = XmlUtil.findChild(element, TAG_PARAMS);

        if (params != null) {
            nodes = XmlUtil.getElements(params, TAG_PARAM);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node  = (Element) nodes.item(i);
                String  name  = XmlUtil.getAttribute(node, "name");
                String  value = XmlUtil.getChildText(node);
                putParamValue(name, value);
            }
        }


        nodes = XmlUtil.getElements(element, TAG_SERVICE);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            addChild(new Service(getRepository(), this, node, i));
        }


        if ((linkId == null) && !haveChildren()) {
            command = XmlUtil.getAttributeFromTree(element, ATTR_COMMAND,
                    (String) null);

            pathProperty = XmlUtil.getAttribute(element, ATTR_PATHPROPERTY,
                    (String) null);

            //Extract it from the command
            if ((pathProperty == null) && (command != null)) {
                int index = command.indexOf("${");
                if (index >= 0) {
                    pathProperty = command.substring(index + 2,
                            command.indexOf("}"));
                }
            }
            if (pathProperty != null) {
                String pathPropertyValue =
                    getRepository().getScriptPathFromTree(pathProperty, null);
                if (pathPropertyValue != null) {
                    pathPropertyValue =
                        getStorageManager().localizePath(pathPropertyValue);
                    if (command == null) {
                        command = pathPropertyValue;
                    } else {
                        command = command.replace(macro(pathProperty),
						  pathPropertyValue);
                    }
                }
            }

            if ((command == null) || (command.indexOf("${") >= 0)) {
                /*
                getLogManager().logError("Service: no command defined:"
                                         + ((command != null)
                                            ? command
                                            : XmlUtil.toString(
                                            element)) + " property:"
                                                + pathProperty);

                */
                return;
            }
        }

        //Look for:
        //java:<class>:<method>
        if ((command != null) && command.equals("util")) {
            command = "java:org.ramadda.service.ServiceUtil:evaluate";
        }

        if ((command != null) && command.startsWith("java:")) {
            List<String> toks      = StringUtil.split(command, ":");
            String       className = toks.get(1);
            if (className.trim().length() == 0) {
                className = "org.ramadda.repository.job.ServiceUtil";
            }
            Class c = Misc.findClass(className);
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class });

            if (ctor != null) {
                commandObject = ctor.newInstance(new Object[] { repository });
            } else {
                commandObject = c.getDeclaredConstructor().newInstance();
            }
            Class[] paramTypes = new Class[] { Request.class, Service.class,
                    ServiceInput.class, List.class };
            commandMethod = Misc.findMethod(commandObject.getClass(),
                                            toks.get(2), paramTypes);
            command = null;
        }

        if (command != null) {
            if ( !new File(command).exists()) {
                getLogManager().logError(
                    "Service: command file does not exist:" + command);

                return;
            }
	    getRepository().addScriptPath(command);
        }


        nodes = XmlUtil.getElements(element, TAG_ARG);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element    node = (Element) nodes.item(i);
            ServiceArg arg  = new ServiceArg(this, node, i);
            args.add(arg);
            if (arg.isEntry()) {
                inputs.add(arg);
            }
        }


        nodes = XmlUtil.getElements(element, TAG_OUTPUT);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element          node   = (Element) nodes.item(i);
            OutputDefinition output = new OutputDefinition(node);
            outputs.add(output);
        }



        enabled = true;

    }


    /**
     * _more_
     *
     * @param xml _more_
     * @param input _more_
     *
     * @throws Exception _more_
     */
    public void toXml(Appendable xml, ServiceInput input) throws Exception {
        StringBuilder attrs = new StringBuilder();
        attr(attrs, ATTR_ENTRY_TYPE, entryType);
        attr(attrs, ATTR_ICON, icon);
        attr(attrs, "outputToStderr", outputToStderr);
        attr(attrs, "immediate", immediate);
        attr(attrs, "ignoreStderr", ignoreStderr);
        attr(attrs, ATTR_CLEANUP, cleanup);
        attr(attrs, "category", category);
        attr(attrs, ATTR_LINK, linkId);
        attr(attrs, ATTR_LABEL, label);
        attr(attrs, ATTR_SERIAL, serial);
        xml.append(XmlUtil.openTag(TAG_SERVICE, attrs.toString()));
        if (Utils.stringDefined(description)) {
            xml.append(XmlUtil.openTag(ATTR_DESCRIPTION));
            xml.append(XmlUtil.getCdata(description));
            xml.append(XmlUtil.closeTag(ATTR_DESCRIPTION));
        }
        if (Utils.stringDefined(processDesc)) {
            xml.append(XmlUtil.openTag("process_description"));
            xml.append(XmlUtil.getCdata(processDesc));
            xml.append(XmlUtil.closeTag("process_description"));
        }
        if (haveChildren()) {
            for (Service child : children) {
                child.toXml(xml, null);
            }
        }
        for (ServiceArg arg : getArgs()) {
            arg.toXml(xml);
        }


        for (OutputDefinition output : outputs) {
            output.toXml(xml);
        }

        if (input != null) {
            writeParamsXml(input, xml);
        }

        xml.append(XmlUtil.closeTag(TAG_SERVICE));
    }


    /**
     * _more_
     *
     * @param xml _more_
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public static void attr(Appendable xml, String name, boolean value)
            throws Exception {
        attr(xml, name, value + "");
    }

    /**
     * _more_
     *
     * @param xml _more_
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public static void attr(Appendable xml, String name, int value)
            throws Exception {
        attr(xml, name, value + "");
    }

    /**
     * _more_
     *
     * @param xml _more_
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public static void attr(Appendable xml, String name, String value)
            throws Exception {
        if (Utils.stringDefined(value)) {
            xml.append(XmlUtil.attr(name, value));
        }
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void putParamValue(String name, Object value) {
        Object v = paramValues.get(name);
        if (v == null) {
            paramValues.put(name, value);
        } else if (v instanceof List) {
            ((List) v).add(value);
        } else {
            List newList = new ArrayList();
            newList.add(v);
            newList.add(value);
            paramValues.put(name, newList);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public Request makeRequest(Request request) {
        //        System.err.println ("request:" + paramValues);
        if (paramValues.size() == 0) {
            return request;
        }
        request = request.cloneMe();
        for (Enumeration keys =
                paramValues.keys(); keys.hasMoreElements(); ) {
            String id    = (String) keys.nextElement();
            Object value = paramValues.get(id);
            if ( !request.defined(id)) {
                request.put(id, value);
            }
        }

        return request;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param argPrefix _more_
     * @param argName _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getRequestValue(Request request, ServiceInput input,
                                   String argPrefix, String argName,
                                   boolean dflt) {
        String v = getRequestValue(request, input, argPrefix, argName,
                                   (String) null);
        if (v == null) {
            return dflt;
        }

        return v.equals("true");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param argPrefix _more_
     * @param argName _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getRequestValue(Request request, String argPrefix,
                                   String argName, boolean dflt) {
        return getRequestValue(request, null, argPrefix, argName, dflt);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param argPrefix _more_
     * @param argName _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getRequestValue(Request request, String argPrefix,
                                  String argName, String dflt) {
        return getRequestValue(request, null, argPrefix, argName, dflt);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param argPrefix _more_
     * @param argName _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getRequestValue(Request request, ServiceInput input,
                                  String argPrefix, String argName,
                                  String dflt) {
        String fullArg = getUrlArg(argPrefix, argName);

        //        System.err.println ("getRequestValue:" + " prefix:" + argPrefix +" name:" + argName +" full:" + fullArg);
        if (request.defined(fullArg)) {
            String value = request.getString(fullArg, dflt);
            if (input != null) {
                input.addParam(fullArg, argPrefix, value);
            }
            debug("getRequestValue: full arg: " + fullArg + "=" + value);

            return value;
        }
        if (request.defined(argName)) {
            String value = request.getString(argName, dflt);
            if (input != null) {
                input.addParam(argName, null, value);
            }
            debug("getRequestValue: part arg: " + argName + "=" + value);

            return value;
        }
        debug("getRequestValue: no value: " + argName);

        return dflt;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param argPrefix _more_
     * @param argName _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public List<String> getRequestValue(Request request, ServiceInput input,
                                        String argPrefix, String argName,
                                        List<String> dflt) {
        String       fullArg = getUrlArg(argPrefix, argName);
        List<String> results = null;
        if (request.defined(fullArg)) {
            results = request.get(fullArg, results);
            if (input != null) {
                for (String value : results) {
                    input.addParam(fullArg, argPrefix, value);
                }
            }

            return results;
        }

        if (request.defined(argName)) {
            results = request.get(argName, results);
            if (input != null) {
                for (String value : results) {
                    input.addParam(argName, null, value);
                }
            }

            return results;
        }
        debug("getRequestValue: no value: " + argName);

        return dflt;
    }


    /**
     * _more_
     *
     *
     * @param service _more_
     */
    public void addChild(Service service) {
        if (children == null) {
            children = new ArrayList<Service>();
        }
        children.add(service);
        service.setParent(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveChildren() {
        return (children != null) && (children.size() > 0);
    }

    /**
     * _more_
     */
    private void initLinkedService() {
        if (link != null) {
            return;
        }
        if (linkId != null) {
            try {
                link = getRepository().getJobManager().getService(linkId);
                if (link == null) {
                    System.out.println("Could not find service:" + linkId
                                       + " " + label + " " + id);
                    System.err.println(XmlUtil.toString(element));
                    enabled = false;
                }
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
    }



    /**
     * _more_
     *
     * @param link _more_
     */
    public void setLinkId(String link) {
        linkId = link;
        initLinkedService();
    }


    /**
     * _more_
     *
     * @param prefix _more_
     * @param tail _more_
     *
     * @return _more_
     */
    public String getUrlArg(String prefix, String tail) {
        return ((prefix == null)
                ? tail
                : prefix + ARG_DELIMITER + tail);
    }






    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveLink() {
        initLinkedService();

        return link != null;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Service getLink() {
        initLinkedService();

        return link;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getImmediate() {
        return immediate;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param argPrefix _more_
     * @param input _more_
     * @param commands _more_
     * @param filesToDelete _more_
     * @param allEntries _more_
     * @param valueMap _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private HashSet<String> addArgs(Request request, String argPrefix,
                                   ServiceInput input, List<String> commands,
                                   List<File> filesToDelete,
                                   List<Entry> allEntries,
				    Hashtable<String, String> valueMap, boolean optional)
            throws Exception {


        if (haveLink()) {
            return link.addArgs(request, argPrefix, input, commands,
                                filesToDelete, allEntries, valueMap,optional);
        }

        HashSet<String> seenGroup   = new HashSet<String>();
        HashSet<String> definedArgs = new HashSet<String>();

        if (linkId != null) {
            System.err.println("Have linkId but no link:" + linkId);

            return definedArgs;
        }

        List<Entry> inputEntries = input.getEntries();
        File        workDir      = input.getProcessDir();

        for (Entry testEntry : inputEntries) {
	    //	    AccessManager.debugAll = true;
            if ( !getAccessManager().canViewFile(request, testEntry)) {
		getLogManager().logSpecial("Service: cannot access file: user:" + request.getUser() + " entry:" + testEntry.getResource());
                throw new AccessException("Can't access file", request);
            }
        }



        Hashtable<String, List<Entry>> entryMap = new Hashtable<String,
                                                      List<Entry>>();

        boolean haveSeenAnEntry = false;


        for (ServiceArg arg : getArgs()) {
            valueMap.put(arg.getName(), "");
        }



        for (ServiceArg arg : getArgs()) {
            if ( !arg.isEntry()) {
                continue;
            }
            String      argName = arg.getName() + "_hidden";
            List<Entry> entries = new ArrayList<Entry>();
            List<String> entryIds = getRequestValue(request, input,
                                        argPrefix, argName,
                                        new ArrayList<String>());
            for (String entryId : entryIds) {
                Entry entry = getEntryManager().getEntry(request, entryId);
                if (entry == null) {
                    System.err.println("Bad entry:" + entryId);

                    throw new IllegalArgumentException(
                        "Could not find entry for arg:" + arg.getLabel()
                        + " entry id:" + entryId);
                }
                entries.add(entry);
            }
            if (entries.size() == 0) {
                if ( !haveSeenAnEntry) {
                    for (Entry entry : input.getEntries()) {
                        if (arg.isApplicable(entry, false)) {
                            entries.add(entry);
			}
                    }
                }
                for (Entry entry : entries) {
                    if ( !getEntryManager().isSynthEntry(entry.getId())) {
                        input.addParam(getUrlArg(argPrefix, argName),
                                       argPrefix, entry.getId());
                    }
                }
            }
            allEntries.addAll(entries);
            entryMap.put(arg.getName(), entries);
            haveSeenAnEntry = true;
        }


        if (inputEntries.size() == 0) {
            inputEntries = allEntries;
            input.setEntries(allEntries);
        }

        Entry  currentEntry = (Entry) Utils.safeGet(inputEntries, 0);

        String cmd          = getCommand();
        if (Utils.stringDefined(cmd)) {
            cmd = applyMacros(request,currentEntry, entryMap, valueMap, workDir, cmd,
                              input.getForDisplay(), null);
            commands.add(cmd);
        }
        addExtraArgs(request, input, commands, true);



        for (ServiceArg arg : getArgs()) {
            if (arg.getDepends() != null) {
                if ( !definedArgs.contains(arg.getDepends())) {
                    //                    System.err.println("Dependency:" + arg.getName() + " " + arg.depends);
                    continue;
                }
            }

            if (arg.getCategory() != null) {
                continue;
            }
            String       argValue = null;
            List<String> values   = null;
            if (arg.isValueArg()) {
                argValue = arg.getValue();
            } else if (arg.isDate()) {
                //TODO: add time
                String dateString = getRequestValue(request, input,
                                        argPrefix, arg.getName(),
                                        (String) null);
                if (Utils.stringDefined(dateString)) {
                    Date date = getRepository().getDateHandler().parseDate(
                                    dateString);
                    argValue = arg.getDateFormat().format(date);
                }
            } else if (arg.isFlag()) {
                if (arg.getGroup() != null) {
                    if ( !seenGroup.contains(arg.getGroup())) {
                        argValue = getRequestValue(request, input, argPrefix,
                                arg.getGroup(), (String) null);
                        if (Utils.stringDefined(argValue)) {
                            seenGroup.add(arg.getGroup());
                        } else {
                            argValue = null;
                        }
                    }
                } else if (getRequestValue(request, input, argPrefix,
                                           arg.getName(), false)) {
                    argValue = arg.getValue();
                }
            } else if (arg.isFile()) {
                //TODO:
                //                String filename = applyMacros(request,currentEntry, valueMap, entryMap, workDir,
                //arg.getFileName(), input.getForDisplay(), arg.getMap());
                //argValue = IOUtil.joinDir(workDir, filename);
            } else if (arg.isEntry()) {
                List<Entry> entries = entryMap.get(arg.getName());
                if ( !arg.isMultiple() && (entries.size() > 1)) {
                    throw new IllegalArgumentException(
                        "Too many entries specified for arg:"
                        + arg.getLabel() + " entries:" + entries);
                } else if ( !arg.isRequired() && (entries.size() == 0)) {
		    if(optional) return null;
                    System.err.println("service arg:" + arg.getName());
                    System.err.println("service entryMap:" + entryMap);
                    throw new IllegalArgumentException(
                        "No entry specified for arg:" + arg.getLabel());
                }


                values = new ArrayList<String>();
                for (Entry entry : entries) {
                    currentEntry = entry;
                    String filePath = getStorageManager().getEntryResourcePath(currentEntry);
                    if (arg.getCopy()) {
                        File newFile =
                            new File(
                                IOUtil.joinDir(
					       input.getProcessDir(),
                                    getStorageManager().getFileTail(
                                        currentEntry)));
                        if ( !newFile.exists()) {
                            IOUtil.copyFile(currentEntry.getFile(), newFile);
                            filesToDelete.add(newFile);
                        }
                        filePath = newFile.toString();
                    }

                    argValue = arg.getValue();
                    argValue = argValue.replace("${entry.file}", filePath);
                    if (arg.getInclude()) {
                        values.add(argValue);
                    }
                }
            } else {
                argValue = getRequestValue(request, input, argPrefix,
                                           arg.getName(), (String) null);

                if ((argValue == null) && (arg.getDefault() != null)) {
                    argValue = arg.getDefault();
                }

                if (argValue != null) {}
            }


            if (arg.isMultiple() && (values != null)) {
                if (arg.getMultipleJoin() != null) {
                    argValue = StringUtil.join(arg.getMultipleJoin(), values);
                    values   = null;
                }
            }


            if ((values == null) && (argValue != null)) {
                values = new ArrayList<String>();
                values.add(argValue);
            }


            int argCnt = 0;
            if (values != null) {
                for (String originalValue : values) {
                    String  value        = originalValue;
                    boolean valueDefined = Utils.stringDefined(value);
                    if ( !valueDefined && arg.getIfDefined()) {
                        //                        System.err.println("Value not defined");
                        continue;
                    }
                    argCnt++;
                    if ( !arg.isEntry()
                            && Utils.stringDefined(arg.getValue())) {
                        value = arg.getValue().replace("${value}", value);
                    }

                    //arg.isRequired()

                    if (Utils.stringDefined(arg.getPrefix())) {
                        commands.add(arg.getPrefix());
                    }

                    if (arg.getFile() != null) {
                        //                            System.err.println ("file:" + arg.getFile() + " " + arg.filePattern);
                        String fileName = applyMacros(request,currentEntry, entryMap,
                                              valueMap, workDir,
                                              arg.getFile(),
                                              input.getForDisplay(),
                                              arg.getMap());


                        fileName = fileName.replace("${value}",
                                originalValue);
                        File destFile = new File(IOUtil.joinDir(workDir,
                                            fileName));
                        int cnt = 0;

                        //                            System.err.println("dest file:" + destFile+" " + destFile.exists());
                        while (destFile.exists()) {
                            cnt++;
                            destFile = new File(IOUtil.joinDir(workDir,
                                    cnt + "_" + fileName));
                        }

                        if (arg.getFilePattern() != null) {
                            String basePattern = applyMacros(request,currentEntry,
                                                     entryMap, valueMap,
                                                     workDir,
                                                     arg.getFilePattern(),
                                                     input.getForDisplay(),
                                                     arg.getMap());


                            basePattern = basePattern.replace("${value}",
                                    originalValue);

                            String pattern = basePattern.replace("${unique}",
                                                 "");
                            File[] files =
                                workDir.listFiles(
                                    (FileFilter) new PatternFileFilter(
                                        pattern));
                            //                                System.err.println("pattern:"+ pattern + " " + files.length);
                            destFile = new File(IOUtil.joinDir(workDir,
                                    fileName));
                            while (files.length > 0) {
                                cnt++;
                                pattern = basePattern.replace("${unique}",
                                        cnt + "");
                                files = workDir.listFiles(
                                    (FileFilter) new PatternFileFilter(
                                        pattern));
                                //                                    System.err.println("pattern:"+ pattern + " " + files.length);
                                destFile = new File(IOUtil.joinDir(workDir,
                                        cnt + "_" + fileName));
                            }
                        }

                        //                            System.err.println("dest file after:" + destFile);
                        value = arg.getValue().replace("${value}", value);
                        value = value.replace("${file}", destFile.getName());
                        value = value.replace("${file.base}",
                                IOUtil.stripExtension(destFile.getName()));
                        value = value.replace("${value}", originalValue);
                        //                            System.err.println("new value:" + value);
		    }
                    value = applyMacros(request,currentEntry, entryMap, valueMap,
                                        workDir, value,
                                        input.getForDisplay(), arg.getMap());
                    valueMap.put(arg.getName(), value);

                    if ( !arg.getInclude()) {
                        continue;
                    }
		    value = value.replace(MACRO_OUTPUTDIR,input.getProcessDir().toString());
                    commands.add(value);
		}
            }

            if (argCnt != 0) {
                definedArgs.add(arg.getName());
            }
            if ((argCnt == 0) && arg.isRequired()) {
                throw new IllegalArgumentException("No entry  specified for:"
                        + arg.getLabel());
            }
        }


        addExtraArgs(request, input, commands, false);

        return definedArgs;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param args _more_
     * @param start _more_
     *
     * @throws Exception _more_
     */
    public void addExtraArgs(Request request, ServiceInput input,
                             List<String> args, boolean start)
            throws Exception {}


    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getLinkXml(ServiceInput input) throws Exception {
        if (haveLink()) {
            return link.getLinkXml(input);
        }


        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.openTag(TAG_SERVICES));
        sb.append("\n");
        sb.append(XmlUtil.openTag(TAG_SERVICE,
                                  XmlUtil.attrs(ATTR_LINK, getId())));

        writeParamsXml(input, sb);

        sb.append(XmlUtil.closeTag(TAG_SERVICE));
        sb.append(XmlUtil.closeTag(TAG_SERVICES));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param input _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void writeParamsXml(ServiceInput input, Appendable sb)
            throws Exception {
        sb.append("\n");
        sb.append(XmlUtil.openTag(TAG_PARAMS));
        sb.append("\n");

        for (String[] param : input.getParams()) {
            sb.append(XmlUtil.tag(TAG_PARAM,
                                  XmlUtil.attrs(ATTR_NAME, param[0]),
                                  XmlUtil.getCdata(param[1])));
        }

        sb.append(XmlUtil.closeTag(TAG_PARAMS));

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCategory() {
        if (category != null) {
            return category;
        }
        if (haveLink()) {
            return link.getCategory();
        }

        return "Services";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getHelp() {
        return null;
    }


    /**
     * Set the Parent property.
     *
     * @param value The new value for Parent
     */
    public void setParent(Service value) {
        parent = value;
    }

    /**
     * Get the Parent property.
     *
     * @return The Parent
     */
    public Service getParent() {
        return parent;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getIcon() {
        if (icon != null) {
            return icon;
        }
        if (link != null) {
            link.getIcon();
        }
        if (haveChildren()) {
            return children.get(0).getIcon();
        }

        return "fa-cog";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        /*
        if (haveLink()) {
            return link.getId();
            }*/

        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTarget() {
        return target;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getNamePattern() {
        return namePattern;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescriptionPattern() {
        return descriptionPattern;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTargetType() {
        return targetType;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param js _more_
     * @param formVar _more_
     *
     * @throws Exception _more_
     */
    public void initFormJS(Request request, Appendable js, String formVar)
            throws Exception {}



    /**
     * _more_
     *
     * @param prefix _more_
     *
     * @return _more_
     */
    public String getPrefix(String prefix) {
        if ( !Utils.stringDefined(prefix)) {
            return this.id;
        } else if ( !Utils.stringDefined(this.id)) {
            return prefix;
        }

        return prefix + ARG_DELIMITER + this.id;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param sb _more_
     * @param argPrefix _more_
     * @param label _more_
     *
     *
     * @throws Exception _more_
     */
    public void addToForm(Request request, ServiceInput input, Appendable sb,
                          String argPrefix, String label)
            throws Exception {


        boolean comingFromForm = request.get(ARG_SERVICEFORM, false);
        if ( !comingFromForm) {
            request = makeRequest(request);
        }

        String myPrefix = getPrefix(argPrefix);
        //        System.err.println("addToForm argPrefix:" + argPrefix + " myPrefix:" + myPrefix);

        if (haveLink()) {
            //            System.err.println("Link:" + link + " " + link.getClass().getName());
            link.addToForm(request, input, sb, myPrefix, getLabel());

            return;
        }

        if (haveChildren()) {
            Service sourceService = input.getSourceService();

            for (Service child : children) {
                StringBuilder tmpSB = new StringBuilder();
                child.addToForm(request, input, tmpSB, myPrefix, label);
                if (tmpSB.length() > 0) {
                    sb.append(HU.p());
                    sb.append(tmpSB);
                }
                input.setSourceService(child);
            }

            input.setSourceService(sourceService);

            return;
        }
        if ( !Utils.stringDefined(label)) {
            label = getLabel();
        }

        addToFormInner(request, myPrefix, input, sb, label);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param prefix _more_
     * @param input _more_
     * @param sb _more_
     * @param label _more_
     *
     *
     * @throws Exception _more_
     */
    private void addToFormInner(Request request, String prefix,
                                ServiceInput input, Appendable sb,
                                String label)
            throws Exception {


        StringBuilder formSB      = new StringBuilder();
        int           blockCnt    = 0;
        CatBuff       catBuff     = null;
        ServiceArg    catArg      = null;
        boolean       anyRequired = false;
        for (int argType = 0; argType <= 1; argType++) {
            for (ServiceArg arg : getArgs()) {
                if (argType == 0) {
                    if ( !arg.isEntry() && !arg.getFirst()) {
                        continue;
                    }
                } else {
                    if (arg.isEntry() || arg.getFirst()) {
                        continue;
                    }
                }

                if (arg.isCategory()) {
                    if ((catBuff != null) && (catBuff.length() > 0)) {
                        processCatBuff(request, formSB, catArg, catBuff,
                                       ++blockCnt);
                    }
                    catArg  = arg;
                    catBuff = new CatBuff();

                    continue;
                }

                if (arg.isValueArg()) {
                    continue;
                }

                if (catBuff == null) {
                    catBuff = new CatBuff();
                    catArg  = null;
                }

                if (arg.isRequired()) {
                    anyRequired = true;
                }
                addArgToForm(request, prefix, input, catBuff, arg);
            }
        }

        if ((catBuff != null) && (catBuff.length() > 0)) {
            processCatBuff(request, formSB, catArg, catBuff, ++blockCnt);
        }
        if (anyRequired) {
            formSB.append(
                "<span class=ramadda-required-label>* required</span>");
        }


        sb.append(HU.open(HU.TAG_DIV,
                                 HU.cssClass("service-form")));


        String rightSide = HU.href(
                               getRepository().getJobManager().getServiceUrl(
                                   request, this), HU.img(
                                   getIconUrl("/icons/application_form.png"),
                                   "View top-level form"));

        rightSide =
            HU.div(rightSide,
                          HU.cssClass("service-form-header-links"));
        sb.append(
            HU.div(
                HU.leftRight(
			     HU.img(getIconUrl(getIcon()),"",HU.attr(HU.ATTR_WIDTH,ICON_WIDTH)) + " " + label,
				    rightSide), HU.cssClass("service-form-header")));


        if (Utils.stringDefined(getDescription())) {
            sb.append(
                HU.div(
                    getDescription(),
                    HU.cssClass("service-form-description")));
        }
        List<Entry> entries = input.getEntries();
        if (false && (entries.size() > 1)) {
            StringBuffer entriesSB = new StringBuffer();
            for (Entry entry : entries) {
                if ( !isApplicable(entry)) {
                    continue;
                }
                entriesSB.append(
                    HU.href(
                        getEntryManager().getEntryURL(request, entry),
                        entry.getName(), " target=\"_help\" "));
                entriesSB.append(HU.br());
            }
            sb.append(
                HU.div(
                    entriesSB.toString(),
                    HU.cssClass("service-form-entries")));
        }


        if (formSB.length() > 0) {
            sb.append(
                HU.div(
                    formSB.toString(),
                    HU.cssClass("service-form-contents")));
        }
        sb.append(HU.close(HU.TAG_DIV));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param argPrefix _more_
     * @param input _more_
     * @param catBuff _more_
     * @param arg _more_
     *
     * @throws Exception _more_
     */
    public void addArgToForm(Request request, String argPrefix,
                             ServiceInput input, CatBuff catBuff,
                             ServiceArg arg)
            throws Exception {


        String        tooltip    = arg.getPrefix();
        StringBuilder inputHtml  = new StringBuilder();
        String        argUrlName = getUrlArg(argPrefix, arg.getName());
        if (arg.isEnumeration()) {
            List<TwoFacedObject> values = arg.getValues();
            if ((values.size() == 0) && (arg.getValuesProperty() != null)) {
                values = (List<TwoFacedObject>) input.getProperty(
                    arg.getValuesProperty(), values);
            }
            if (values.size() == 0) {
                values = (List<TwoFacedObject>) input.getProperty(argUrlName
                        + ".values", values);
            }

            if (arg.getAddAll()) {
                values = new ArrayList<TwoFacedObject>(values);
                values.add(0, new TwoFacedObject("--all--", ""));
            } else if (arg.getAddNone()) {
                values = new ArrayList<TwoFacedObject>(values);
                values.add(0, new TwoFacedObject("--none--", ""));
            }

            String extra = "";
            if (arg.isMultiple()) {
                extra = " MULTIPLE SIZE=" + ((arg.getSize() > 0)
                                             ? arg.getSize()
                                             : 4) + " ";
            }

            List selected = request.get(argUrlName, new ArrayList<String>());
            //                                        request.get(arg.getName(),
            //                                                    Misc.newList(arg.getDefault())));
            //            System.err.println("arg.name:" + arg.getName());
            //            System.err.println("argUrlName:" + argUrlName);
            //            System.err.println("selected:" + selected);
            //            System.err.println("request:" + request);
            inputHtml.append(HU.select(argUrlName, values, selected,
                    extra, 100));
        } else if (arg.isFlag()) {
            if (arg.getGroup() != null) {
                boolean selected =
                    getRequestValue(request, argPrefix, arg.getGroup(),
                                    arg.getDefault()).equals(arg.getValue());
                inputHtml.append(HU.radio(getUrlArg(argPrefix,
                        arg.getGroup()), arg.getValue(), selected));
            } else {
                inputHtml.append(HU.checkbox(argUrlName, "true",
                        getRequestValue(request, argPrefix, arg.getName(),
                                        arg.getDefault().equals("true"))));
            }

            inputHtml.append(HU.space(2));
            inputHtml.append(arg.getHelp());
            if (arg.getSameRow()) {
                catBuff.appendToCurrentRow(inputHtml.toString());
            } else {
                catBuff.addRow("", inputHtml.toString(), null);
            }

            return;
        } else if (arg.isDate()) {
            //TODO: add a time field
            String     dateProp = arg.getValuesProperty();
            List<Date> dates    = new ArrayList<Date>();
            if (arg.getValuesProperty() != null) {
                dates =
                    (List<Date>) input.getProperty(arg.getValuesProperty(),
                        dates);
            }
            inputHtml.append(
                getRepository().getDateHandler().makeDateInput(
                    request, argUrlName, "searchform", null, null, false,
                    dates));
        } else if (arg.isFile()) {
            //noop
        } else if (arg.isEntry()) {
            List<Entry> entries      = input.getEntries();
            Entry       primaryEntry = ((entries.size() == 0)
                                        ? null
                                        : entries.get(0));

            if ((input.getSourceService() != null)
                    && (input.getSourceService().getOutputs().size() > 0)) {
                return;
            }
            if ((primaryEntry != null) && arg.isPrimaryEntry()) {
                return;
            }

            if (arg.getEntryType() != null) {
                request.put(ARG_ENTRYTYPE, arg.getEntryType());
            }
            String elementId = HU.getUniqueId("select_");
            inputHtml.append(OutputHandler.getSelect(request, elementId,
						     msg("Select"), true, null));

            String entryLabel = "";

            String entryId    = "";

            String argName    = arg.getName() + "_hidden";
            Entry  entryArg   = getEntry(request, argPrefix, arg);
            if (entryArg != null) {
                entryLabel = entryArg.getName();
                entryId    = entryArg.getId();
            }

            inputHtml.append(HU.hidden(getUrlArg(argPrefix, argName),
                    entryId, HU.id(elementId + "_hidden")));
            inputHtml.append(HU.space(1));
            inputHtml.append(HU.disabledInput(argUrlName, entryLabel,
                    HU.SIZE_60 + HU.id(elementId)));
            //                inputHtml.append(HU.disabledInput(argUrlName,
            //                                                         getRequestValue(request, argPrefix, arg.getName(), ""),
            //                                                         HU.SIZE_60 + HU.id(elementId)));
            request.remove(ARG_ENTRYTYPE);

        } else {
            String extra = HU.attr(HU.ATTR_SIZE,
                                          "" + arg.getSize());
            if (arg.getPlaceHolder() != null) {
                extra += HU.attr("placeholder", arg.getPlaceHolder());
            }
            inputHtml.append(
                HU.input(
                    argUrlName,
                    getRequestValue(
                        request, argPrefix, arg.getName(),
                        arg.getDefault()), extra));
        }
        if (inputHtml.length() == 0) {
            return;
        }
        if (arg.isRequired()) {
            inputHtml.append(HU.space(1));
            inputHtml.append("<span class=ramadda-required-label>*</span>");
        }
        if (Utils.stringDefined(arg.getHelp())) {
            inputHtml.append(HU.space(2));
            inputHtml.append(arg.getHelp());
        }

        if (arg.getSameRow()) {
            catBuff.appendToCurrentRow(inputHtml.toString());
        } else {
            catBuff.addRow(arg.getLabel(), inputHtml.toString(), null);
        }
        //        makeFormEntry(catBuff, arg.getLabel(), inputHtml.toString(), arg.getHelp());


    }


    /**
     * _more_
     *
     * @param request _more_
     * @param argPrefix _more_
     * @param arg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(Request request, String argPrefix, ServiceArg arg)
            throws Exception {
        String argName = arg.getName() + "_hidden";
        String entryId = getRequestValue(request, argPrefix, argName, "");
        if (Utils.stringDefined(entryId)) {
            return getEntryManager().getEntry(request, entryId);
        }

        return null;
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     * @param col1 _more_
     * @param help _more_
     *
     * @throws Exception _more_
     */
    private void makeFormEntry(Appendable sb, String label, String col1,
                               String help)
            throws Exception {
        if (help != null) {
            help = HU.div(help,
                                 HU.cssClass("service-form-help"));
            sb.append(HU.formEntryTop(Utils.stringDefined(label)
                                             ? msgLabel(label)
                                             : "", col1, help));

        } else {
            sb.append(HU.formEntryTop(Utils.stringDefined(label)
                                             ? msgLabel(label)
                                             : "", col1, 2));
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param catArg _more_
     * @param catBuff _more_
     * @param blockCnt _more_
     *
     * @throws Exception _more_
     */
    private void processCatBuff(Request request, Appendable sb,
                                ServiceArg catArg, CatBuff catBuff,
                                int blockCnt)
            throws Exception {
        if (catArg != null) {
            String html = header(catArg.getCategory());
            /*
            String desc = catArg.getValue();
            if (Utils.stringDefined(desc)) {
                html += desc;
                html += HU.br();
            }
            */
            sb.append(html);
        }
        StringBuilder formSB = new StringBuilder(HU.formTable());
        catBuff.addToForm(formSB);
        formSB.append(HU.formTableClose());
        if (true || (blockCnt == 1)) {
            sb.append(formSB);
        } else {
            sb.append(HU.makeShowHideBlock("", formSB.toString(),
                    true || (blockCnt == 1)));
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        if (haveLink()) {
            return link.isEnabled();
        }
        if (haveChildren()) {
            for (Service child : children) {
                if ( !child.isEnabled()) {
                    return false;
                }
            }

            return true;
        }

        return enabled;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean requiresMultipleEntries() {
        if (requiresMultipleEntries != null) {
            return requiresMultipleEntries;
        }
        if (haveLink()) {
            requiresMultipleEntries =
                Boolean.valueOf(link.requiresMultipleEntries());
        } else if (haveChildren()) {
            if (serial) {
                requiresMultipleEntries =
                    Boolean.valueOf(children.get(0).requiresMultipleEntries());
            } else {
                for (Service child : children) {
                    if ( !child.isEnabled()) {
                        requiresMultipleEntries = Boolean.valueOf(false);

                        break;
                    }
                }
            }
        } else {
            for (ServiceArg arg : getArgs()) {
                if (arg.isEntry()) {
                    requiresMultipleEntries = Boolean.valueOf(arg.isMultiple());

                    break;
                }
            }
        }
        if (requiresMultipleEntries == null) {
            requiresMultipleEntries = Boolean.valueOf(false);
        }

        return requiresMultipleEntries;
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public void collectArgs(List<ServiceArg> args) {
        if (haveLink()) {
            link.collectArgs(args);

            return;
        }
        if (haveChildren()) {
            for (Service child : children) {
                child.collectArgs(args);
            }
        }

        args.addAll(this.args);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getOutputToStderr() {
        if (haveLink()) {
            return link.getOutputToStderr();
        }

        return outputToStderr;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIgnoreStderr() {
        if (haveLink()) {
            return link.getIgnoreStderr();
        }

        return ignoreStderr;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<ServiceArg> getArgs() {
        return args;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<OutputDefinition> getOutputs() {
        if (haveLink()) {
            return link.getOutputs();
        }

        return outputs;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<ServiceArg> getInputs() {
        return inputs;
    }


    /**
     * Can we handle this type of ServiceInput?
     *
     * @param dpi ServiceInput
     * @return true if we can handle
     */
    public boolean canHandle(ServiceInput dpi) {
        List<Entry> entries = dpi.getEntries();
        if (entries.size() == 0) {
            return false;
        }

        return isApplicable(entries.get(0));
    }


    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     */
    public boolean isApplicable(List<Entry> entries) {
        if ( !requiresMultipleEntries()) {
            return false;
        }
        if (entries == null) {
            return false;
        }
        int cnt = 0;
        for (Entry entry : entries) {
            if (isApplicableInner(entry)) {
                cnt++;
                if (cnt > 1) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean isApplicable(Entry entry) {
        if (requiresMultipleEntries()) {
            return false;
        }

        return isApplicableInner(entry);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private boolean isApplicableInner(Entry entry) {
        if (haveLink()) {
            return link.isApplicable(entry);
        }

        if (haveChildren()) {
            return children.get(0).isApplicable(entry);
        }

        boolean debug = false;
        if (debug) {
            System.err.println("isApplicable:" + getLabel() + " " + command);
        }


        for (ServiceArg input : inputs) {
            //            boolean debug = true;
            if (input.isApplicable(entry, debug)) {
                return true;
            }
        }
        if (inputs.size() > 0) {
            return false;
        }
        if (entryType != null) {
            return entry.getTypeHandler().isType(entryType);
        }

        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCommand() {
        if (haveLink()) {
            return link.getCommand();
        }

        return command;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     */
    public String getDescription() {
        if (haveLink()) {
            link.getDescription();
        }

        return description;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getProcessDescription() {
        if (Utils.stringDefined(processDesc)) {
            return processDesc;
        }
        if (haveLink()) {
            return link.getProcessDescription();
        }

        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        if (Utils.stringDefined(label)) {
            return label;
        }
        if (haveLink()) {
            return link.getLabel();
        }

        if (id != null) {
            return id.replaceAll("_", " ");
        }

        return "Service";
    }


    /**
     * _more_
     *
     * @param l _more_
     */
    public void setLabel(String l) {
        label = l;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param argPrefix _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ServiceOutput evaluate(Request request, ServiceInput input,
                                  String argPrefix)
            throws Exception {
	return evaluate(request, input,argPrefix,optional);
    }

    public ServiceOutput evaluate(Request request, ServiceInput input,
                                  String argPrefix,boolean optional)
            throws Exception {	

        String  myPrefix       = getPrefix(argPrefix);

        boolean comingFromForm = request.get(ARG_SERVICEFORM, false);

        if ( !comingFromForm) {
            request = makeRequest(request);
        }

        if (haveLink()) {
            return link.evaluate(request, input, myPrefix,optional);
        }

        ServiceOutput myOutput      = new ServiceOutput();
        List<File>    filesToDelete = new ArrayList<File>();
        HashSet<File> existingFiles = new HashSet<File>();
        for (File f : input.getProcessDir().listFiles()) {
            existingFiles.add(f);
        }
        List<Entry>   entries  = input.getEntries();

        HashSet<File> newFiles = new HashSet<File>();
        if (haveChildren()) {
            ServiceOutput childOutput = null;
            ServiceInput  childInput  = input;

            for (Service child : children) {
                //                System.err.println("Input:" + childInput.getEntries());
                childOutput = child.evaluate(request, childInput, myPrefix);
                if ( !childOutput.isOk()) {
                    return childOutput;
                }
                if ( !serial) {
                    if (childOutput.getResultsShownAsText()) {
                        myOutput.setResultsShownAsText(true);
                    }
                    myOutput.getEntries().addAll(childOutput.getEntries());
                    myOutput.append(childOutput.getResults());
                } else {
                    childInput = childInput.makeInput(childOutput);

                }
            }

            //If we are  serial then we only add the last command's entry (or add them all?)
            if (serial && (childOutput != null)) {
                myOutput.getEntries().addAll(childOutput.getEntries());
                if (childOutput.getResultsShownAsText()) {
                    myOutput.setResultsShownAsText(true);
                }
                myOutput.append(childOutput.getResults());
            }


            for (Entry newEntry : myOutput.getEntries()) {
                newFiles.add(newEntry.getFile());
            }


            if (cleanup) {
                for (File f : input.getProcessDir().listFiles()) {
                    if (f.getName().startsWith(".")) {
                        continue;
                    }
                    if (existingFiles.contains(f) || newFiles.contains(f)) {
                        continue;
                    }
		    //                    System.err.println("Service.evaluate: deleting:" + f);
                    f.delete();
                }
            }
            if (input.getForDisplay()) {
                return myOutput;
            }

            return myOutput;
        }


        List<String>              commands   = new ArrayList<String>();
        List<Entry>               allEntries = new ArrayList<Entry>();
        Hashtable<String, String> valueMap   = new Hashtable<String,
                                                   String>();
        HashSet<String> definedArgs = this.addArgs(request, myPrefix, input,
                                          commands, filesToDelete,
						   allEntries, valueMap,optional);

	if(definedArgs==null) {
	    return null;
	}
        if (entries.size() == 0) {
            entries = allEntries;
        }

        Entry currentEntry = (Entry) Utils.safeGet(entries, 0);


        if (input.getForDisplay()) {
            commands.set(0, IOUtil.getFileTail(commands.get(0)));
            myOutput.append(StringUtil.join(" ", commands));
            myOutput.append("\n");

            return myOutput;
        }

        String errMsg = "";
        String outMsg = "";
        File stdoutFile = new File(IOUtil.joinDir(input.getProcessDir(),
                              "." + getId() + ".stdout"));
        File stderrFile = new File(IOUtil.joinDir(input.getProcessDir(),
                              "." + getId() + ".stderr"));


	
	//	System.err.println("commands:" + commands);
        //        System.out.println(getLinkXml(input));
        if (commandObject != null) {
            commandMethod.invoke(commandObject, new Object[] { request, this,
                    input, commands });
        } else {
	    try {
		JobManager.CommandResults results =
		    getRepository().getJobManager().executeCommand(commands,
								   null, input.getProcessDir(), -1,
								   new PrintWriter(stdoutFile), new PrintWriter(stderrFile));
	    } catch(Exception exc) {
		System.err.println("Error evaluating service:" + commands);
		throw exc;
	    }
        }
        if (stderrFile.exists()) {
            errMsg = IOUtil.readContents(stderrFile);
        }
	//	errMsg = "Mar 03, 2023 1:08:26 PM org.apache.pdfbox.pdmodel.font.PDType1Font <init>\nWARNING: Using fallback font LiberationSans for base font Symbol\nMar 03, 2023 1:46:41 PM org.apache.pdfbox.pdmodel.font.PDType0Font toUnicode\nWARNING: No Unicode mapping for CID+72 (72) in font SGGFTZ+HelveticaNeue-Light\nMar 03, 2023 1:46:41 PM org.apache.pdfbox.pdmodel.font.PDType0Font toUnicode\nWARNING: No Unicode mapping for CID+74 (74) in font SGGFTZ+HelveticaNeue-Light\nMar 03, 2023 1:46:41 PM org.apache.pdfbox.pdmodel.font.PDType0Font toUnicode\nWARNING: No Unicode mapping for CID+76 (76) in font SGGFTZ+HelveticaNeue-Light";
	//	errMsg="Mar 03, 2023 2:01:55 PM org.apache.pdfbox.pdmodel.font.PDType0Font toUnicode";
	boolean debugErr = false;
        if (Utils.stringDefined(errMsg)) {
	    if(debugErr)
		System.err.println("ignore:" + ignore);
	    int okCnt=0;
	    List<String> lines = Utils.split(errMsg,"\n",true,true);
	    for(String line:lines) {
		boolean lineOk = false;
		if(debugErr)
		    System.err.println("line:" + line);
		if(ignore!=null) {
		    for(String i: ignore) {
			if(line.toLowerCase().matches(i) || line.matches(i)) {
			    lineOk = true;
			    if(debugErr)
				System.err.println("\tmatch: " + i);
			    break;
			} else {
			    if(debugErr)
				System.err.println("\tno match: " + i);
			}
		    }
		} else {
		    if(line.toLowerCase().startsWith("warning")) {
			lineOk = true;
		    }
		}
		if(lineOk) {
		    okCnt++;
		    if(debugErr)
			System.err.println("\tOk");
		} else {
		    if(debugErr)
			System.err.println("\tNot ok");
		}
	    }
	    if(okCnt==lines.size()) errMsg = null;
	}

        if (Utils.stringDefined(errMsg)) {
            if (getOutputToStderr()) {
                myOutput.append(errMsg);
                myOutput.append("\n");
            } else {
                if ( !getIgnoreStderr()) {
                    if (errorPattern != null) {
                        if (errMsg.matches(errorPattern)) {
                            myOutput.setOk(false);
                            myOutput.append(errMsg);

                            return myOutput;
                        } else {
                            System.err.println("Skipping stderr:" + errMsg);
                            getLogManager().logInfo(
                                "Service: ignoring stderr:" + errMsg);
                        }
                    } else {
                        //If there is an error then
			myOutput.setOk(false);
                        myOutput.append(errMsg);
                        return myOutput;
                    }
                }
            }
	}
        boolean       setResultsFromStdout = true;
        HashSet<File> seen                 = new HashSet<File>();

        for (File f : filesToDelete) {
	    //            System.err.println("Service: deleting file:" + f);
            f.delete();
        }

	if(debug)
	    System.err.println("Service.evaluate");

        for (OutputDefinition output : getOutputs()) {
            String depends = output.getDepends();
            if (depends != null) {
                if (depends.startsWith("!")) {
                    if (definedArgs.contains(depends.substring(1))) {
                        continue;
                    }
                } else {
                    if ( !definedArgs.contains(depends)) {
                        continue;
                    }
                }
	    }

            if (output.getShowResults()) {
                setResultsFromStdout = false;
                myOutput.setResultsShownAsText(true);
                if (output.getUseStdout() && stdoutFile.exists()) {
		    String results = IOUtil.readContents(stdoutFile);
		    //Strip out any files paths, etc.
		    results = results.replaceAll(getStorageManager().getStorageDir(),"...");
                    myOutput.append(results);
                } else {}

                continue;
            }


            File[] files = null;
            if (output.getUseStdout()) {
                setResultsFromStdout = false;
                String filename = applyMacros(request,currentEntry, null, valueMap,
                                      input.getProcessDir(),
                                      output.getFilename(),
                                      input.getForDisplay(), output.getMap());
                File destFile =
                    new File(IOUtil.joinDir(input.getProcessDir(), filename));
                IOUtil.moveFile(stdoutFile, destFile);
                files = new File[] { destFile };
            }
            final String thePattern = applyMacros(request,currentEntry, null, null,
                                          input.getProcessDir(),
                                          output.getPattern(),
                                          input.getForDisplay(),
                                          output.getMap());


            if (files == null) {
		if(debug) System.err.println("Service:" + this +" process dir:" +input.getProcessDir());
                files = input.getProcessDir().listFiles(new FileFilter() {
                    public boolean accept(File f) {
                        String name = f.getName();
			if(debug) System.err.println("Service: file:" + f);
                        if (name.startsWith(".")) {
                            return false;
                        }
                        if (thePattern == null) {
                            return true;
                        }
                        if (name.toLowerCase().matches(thePattern)) {
                            return true;
                        }

                        return false;
                    }
                });
            }



            for (File file : files) {
		if(debug)
		    System.err.println("Service: file:" + file +" " + file.exists());
                if (input.haveSeenFile(file)) {
		    System.err.println("Service: file seen it");
                    continue;
                }
                input.addSeenFile(file);
                StringBuilder entryXml = new StringBuilder();
                entryXml.append(XmlUtil.tag("entry",
                                            XmlUtil.attrs("name",
                                                file.getName(), "type",
                                                    (output.getEntryType()
                                                        != null)
                        ? output.getEntryType()
                        : TypeHandler.TYPE_FILE)));
                IOUtil.writeFile(getEntryManager().getEntryXmlFile(file),
                                 entryXml.toString());

                TypeHandler typeHandler =
                    getRepository().getTypeHandler(output.getEntryType());
                Entry newEntry =
                    typeHandler.createEntry(getRepository().getGUID());
                newEntry.setDate(new Date().getTime());
                newEntry.setName(file.getName());
                newEntry.setResource(new Resource(file, Resource.TYPE_FILE));
                if (input.getPublish()) {
		    if(debug)
			System.err.println("publish:" + newEntry +" current:" + currentEntry);
                    getEntryManager().processEntryPublish(request, file,
                            newEntry, currentEntry, "derived from");

                } else {
		    if(debug)
			System.err.println("Service: no publish:" + newEntry);
                    newEntry
                        .setId(getEntryManager().getProcessFileTypeHandler()
                            .getSynthId(getEntryManager().getProcessEntry(),
                                        input.getProcessDir().toString(),
                                        file));
                }
                myOutput.addEntry(newEntry);
            }
        }

        if (setResultsFromStdout && stdoutFile.exists()) {
            myOutput.append(IOUtil.readContents(stdoutFile));
        }

        return myOutput;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param output _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addOutput(Request request, ServiceInput input,
                          ServiceOutput output, Appendable sb)
            throws Exception {
        if (haveLink()) {
            link.addOutput(request, input, output, sb);

            return;
        }

        int cnt = 0;
        for (Entry entry : input.getEntries()) {
            if (cnt++ == 0) {
                sb.append(
                    HU.open(
                        HU.TAG_DIV,
                        HU.cssClass("service-output-header")));
            } else {
                sb.append(HU.br());
            }
            sb.append(HU.href(getEntryManager().getEntryURL(request,
                    entry), entry.getName()));
        }
        if (cnt > 0) {
            sb.append(HU.close(HU.TAG_DIV));
        }
        sb.append("<div class=service-output>");
        sb.append("<pre>");
        sb.append(output.getResults());
        sb.append("</pre>");
        sb.append("</div>");
    }


    /**
     * _more_
     */
    public void ensureSafeServices() {
        if (command != null) {
            throw new IllegalArgumentException(
                "Service cannot have a command:" + command);
        }
        if (haveChildren()) {
            for (Service child : children) {
                child.ensureSafeServices();
            }
        }
    }

    /**
     * _more_
     *
     * @param d _more_
     */
    public void setDescription(String d) {
        description = d;
    }


    /**
     *  Set the Serial property.
     *
     *  @param value The new value for Serial
     */
    public void setSerial(boolean value) {
        serial = value;
    }

    /**
     *  Get the Serial property.
     *
     *  @return The Serial
     */
    public boolean getSerial() {
        return serial;
    }




    /**
     * _more_
     *
     * @param outputs _more_
     */
    public void getAllOutputs(List<OutputDefinition> outputs) {
        if (haveChildren()) {
            for (Service child : children) {
                child.getAllOutputs(outputs);
            }
        }
        outputs.addAll(this.getOutputs());
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String macro(String s) {
        return "${" + s + "}";
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param entryMap _more_
     * @param valuesSoFar _more_
     * @param workDir _more_
     * @param value _more_
     * @param forDisplay _more_
     * @param extraMap _more_
     *
     * @return _more_
     */
    public String applyMacros(Request request,Entry entry,
                              Hashtable<String, List<Entry>> entryMap,
                              Hashtable<String, String> valuesSoFar,
                              File workDir, String value, boolean forDisplay,
                              Hashtable<String, String> extraMap) throws Exception {

        if (value == null) {
            return null;
        }


        value = value.replace("${workdir}", forDisplay
                                            ? "&lt;working directory&gt;"
                                            : workDir.toString());

        if (valuesSoFar != null) {
            for (Enumeration keys = valuesSoFar.keys();
                    keys.hasMoreElements(); ) {
                String id = (String) keys.nextElement();
                String v  = valuesSoFar.get(id);
                value = value.replace("${" + id + "}", v);
            }
        }
        if (entryMap != null) {
            for (Enumeration keys =
                    entryMap.keys(); keys.hasMoreElements(); ) {
                String id = (String) keys.nextElement();
                for (Entry otherEntry : entryMap.get(id)) {
                    value = applyMacros(request, otherEntry, id, value, forDisplay);
                }
            }
        }

        if (extraMap != null) {
            for (Enumeration keys =
                    extraMap.keys(); keys.hasMoreElements(); ) {
                String id = (String) keys.nextElement();
                String v  = extraMap.get(id);
                value = value.replace(id, v);
            }

        }


        //        System.err.println("Apply macros:" +entry);
        if (entry != null) {
            value = applyMacros(request,entry, "entry", value, forDisplay);
        }

        return value;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param id _more_
     * @param value _more_
     * @param forDisplay _more_
     *
     * @return _more_
     */
    private String applyMacros(Request request, Entry entry, String id, String value,
                               boolean forDisplay) throws Exception {
        List<Column> columns = entry.getTypeHandler().getColumns();
        if (columns != null) {
            for (Column column : columns) {
                Object columnValue =
                    entry.getValue(request,  column.getName());
                if (columnValue != null) {
                    value = value.replace("${" + id + ".attr."
                                          + column.getName() + "}", ""
                                              + columnValue);
                } else {
                    value = value.replace("${" + id + ".attr."
                                          + column.getName() + "}", "");
                }
            }
        }


        String fileTail = getStorageManager().getFileTail(entry);
        value = value.replace("${" + id + ".id}", entry.getId());
        value = value.replace("${" + id + ".file}", forDisplay
			      ? getStorageManager().getFileTail(entry)
			      : getStorageManager().getEntryResourcePath(entry));
        //? not sure what the macros should be
        //            value = value.replace(macro("entry.file.base"),
        //                                  IOUtil.stripExtension(entry.getName()));
        value = value.replace("${" + id + ".file.base}",
                              IOUtil.stripExtension(fileTail));
        value = value.replace("${" + id + ".file.suffix}",
                              IOUtil.getFileExtension(fileTail).replace(".",
                                  ""));

        return value;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Sep 23, '14
     * @author         Enter your name here...
     */
    private class CatBuff {

        /** _more_ */
        List<List<StringBuilder>> rows = new ArrayList<List<StringBuilder>>();

        /**
         * _more_
         *
         * @param label _more_
         * @param value _more_
         * @param value2 _more_
         *
         * @return _more_
         */
        public List<StringBuilder> addRow(String label, String value,
                                          String value2) {
            List<StringBuilder> row = new ArrayList<StringBuilder>();
            row.add(new StringBuilder(label));
            row.add(new StringBuilder(value));
            if (value2 != null) {
                row.add(new StringBuilder(value2));
            }
            rows.add(row);

            return row;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int length() {
            return rows.size();
        }

        /**
         * _more_
         *
         * @param row _more_
         */
        public void addRow(List<StringBuilder> row) {
            rows.add(row);
        }

        /**
         * _more_
         *
         * @param v _more_
         */
        public void appendToCurrentRow(String v) {
            List<StringBuilder> row = rows.get(rows.size() - 1);
            row.get(1).append(v);
        }



        /**
         * _more_
         *
         * @param sb _more_
         *
         * @throws Exception _more_
         */
        public void addToForm(Appendable sb) throws Exception {
            for (List<StringBuilder> row : rows) {
                String label  = row.get(0).toString();
                String value  = row.get(1).toString();
                String value2 = ((row.size() > 2)
                                 ? row.get(2).toString()
                                 : null);
                makeFormEntry(sb, label, value, value2);
            }
        }


    }



    /**
     * _more_
     *
     * @return _more_
     */
    public Element getElement() {
        return element;
    }


    /**
     * Set the ServiceEntry property.
     *
     * @param value The new value for ServiceEntry
     */
    public void setServiceEntry(Entry value) {
        serviceEntry = value;
    }

    /**
     * Get the ServiceEntry property.
     *
     * @return The ServiceEntry
     */
    public Entry getServiceEntry() {
        return serviceEntry;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return getLabel() + " " + id +" " + command;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getMaxFileSize() {
        return maxFileSize;
    }

    public boolean getOptional() {
	return optional;
    }


}
