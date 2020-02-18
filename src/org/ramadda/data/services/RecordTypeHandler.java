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

package org.ramadda.data.services;


import org.ramadda.data.point.PointFile;
import org.ramadda.data.point.PointMetadataHarvester;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordFileContext;
import org.ramadda.data.record.RecordFileFactory;
import org.ramadda.data.record.RecordVisitorGroup;

import org.ramadda.data.record.VisitInfo;

import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.PointEntry;

import org.ramadda.data.services.RecordEntry;

import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.grid.LatLonGrid;



import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.io.File;

import java.lang.reflect.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
public abstract class RecordTypeHandler extends BlobTypeHandler implements RecordConstants,
        RecordFileContext {

    /** _more_ */
    public static final int IDX_RECORD_COUNT = 0;

    /** _more_ */
    public static final int IDX_PROPERTIES = 1;

    /** _more_ */
    public static final int IDX_LAST = IDX_PROPERTIES;

    /** _more_ */
    private RecordFileFactory recordFileFactory;

    /** _more_ */
    private RecordOutputHandler recordOutputHandler;


    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     */
    public RecordTypeHandler(Repository repository, String type,
                             String description) {
        super(repository, type, description);
    }




    /**
     * _more_
     *
     * @param repository ramadda
     * @param node _more_
     * @throws Exception On badness
     */
    public RecordTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getContextNamespace() {
        return getTypeProperty("record.namespace", "record");
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param key _more_
     *
     * @return _more_
     */
    public String getFieldProperty(String field, String key) {
        key = getContextNamespace() + "." + field + "." + key;
        String v = getRepository().getProperty(key);
        if ((v != null) && (v.trim().length() > 0)) {
            return v;
        }

        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public RecordOutputHandler getRecordOutputHandler() {
        if (recordOutputHandler == null) {
            try {
                recordOutputHandler = doMakeRecordOutputHandler();
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return recordOutputHandler;
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("point_metadata")) {
            RecordOutputHandler outputHandler = getRecordOutputHandler();
            StringBuffer        sb            = new StringBuffer();
            outputHandler.getFormHandler().getEntryMetadata(request,
                    outputHandler.doMakeEntry(request, entry), sb);

            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordOutputHandler doMakeRecordOutputHandler() throws Exception {
        return new RecordOutputHandler(getRepository(), null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToProcessingForm(Request request, Entry entry,
                                    Appendable sb)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param recordEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean includedInRequest(Request request, RecordEntry recordEntry)
            throws Exception {
        return true;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tabTitles _more_
     * @param tabContents _more_
     */
    public void addToInformationTabs(Request request, Entry entry,
                                     List<String> tabTitles,
                                     List<String> tabContents) {
        //        super.addToInformationTabs(request, entry, tabTitles, tabContents);
        try {
            RecordOutputHandler outputHandler = getRecordOutputHandler();
            if (outputHandler != null) {
                tabTitles.add(msg("File Format"));
                StringBuffer sb = new StringBuffer();
                RecordEntry recordEntry = outputHandler.doMakeEntry(request,
                                              entry);
                outputHandler.getFormHandler().getEntryMetadata(request,
                        recordEntry, sb);
                tabContents.add(sb.toString());
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param originalFile _more_
     * @param force _more_
     *
     * @throws Exception _more_
     */
    public void initializeRecordEntry(Entry entry, File originalFile,
                                      boolean force)
            throws Exception {

        if ( !force && anySuperTypesOfThisType()) {
            return;
        }
        Hashtable existingProperties = getRecordProperties(entry);
        if ((existingProperties != null) && (existingProperties.size() > 0)) {
            return;
        }

        //Look around for properties files that define
        //the crs, fields for text formats, etc.
        Hashtable properties =
            RecordFile.getPropertiesForFile(originalFile.toString(),
                                            PointFile.DFLT_PROPERTIES_FILE);


        //Make the properties string
        String   contents = makePropertiesString(properties);
        Object[] values   = entry.getTypeHandler().getEntryValues(entry);
        //Append the properties file contents
        if (values[IDX_PROPERTIES] != null) {
            values[IDX_PROPERTIES] = "\n" + contents;
        } else {
            values[IDX_PROPERTIES] = contents;
        }
    }

    /**
     * _more_
     *
     * @param properties _more_
     *
     * @return _more_
     */
    public String makePropertiesString(Hashtable properties) {
        StringBuffer sb = new StringBuffer();
        for (java.util.Enumeration keys = properties.keys();
                keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            sb.append(key);
            sb.append("=");
            sb.append(properties.get(key));
            sb.append("\n");
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param msg _more_
     */
    public void log(String msg) {
        getRepository().getLogManager().logInfo("RecordTypeHandler:" + msg);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryCategory(Entry entry) {
        return getTypeProperty("entry.category", "");
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param originalEntry _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeCopiedEntry(Entry entry, Entry originalEntry)
            throws Exception {
        super.initializeCopiedEntry(entry, originalEntry);
        initializeNewEntry(null, entry);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Hashtable getRecordProperties(Entry entry) throws Exception {
        Object[]  values = entry.getTypeHandler().getEntryValues(entry);
        String    propertiesString = (values[IDX_PROPERTIES] != null)
                                     ? values[IDX_PROPERTIES].toString()
                                     : "";

        String    typeProperties   = getRecordPropertiesFromType(entry);

        Hashtable p                = null;


        if (typeProperties != null) {
            if (p == null) {
                p = new Hashtable();
            }
            p.putAll(Utils.getProperties(typeProperties));
        }


        if (propertiesString != null) {
            if (p == null) {
                p = new Hashtable();
            }
            p.putAll(Utils.getProperties(propertiesString));
        }

        return p;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getRecordPropertiesFromType(Entry entry) throws Exception {
        return getTypeProperty("record.properties", (String) null);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        Hashtable properties = getRecordProperties(entry);
        RecordFile recordFile = doMakeRecordFile(entry, properties,
                                    request.getDefinedProperties());
        if (recordFile == null) {
            return null;
        }

        File file = null;
        if (getTypeProperty("record.file.cacheok", true)) {
            String      suffix = "";
            List<Macro> macros = getMacros(entry);
            if (macros != null) {
                for (Macro macro : macros) {
                    String v = request.getString("macro_" + macro.name,
                                   macro.dflt);
                    v      = v.replaceAll("\\.", "_").replaceAll("/", "_");
                    suffix += "_" + v;
                }
            }
            String filename = "record_" + entry.getId() + "_"
                              + entry.getChangeDate() + suffix + ".csv";
            //      System.err.println("cache file:" + filename);
            file = getRepository().getEntryManager().getCacheFile(entry,
                    filename);
            recordFile.setCacheFile(file);
        }


        //Explicitly set the properties to force a call to initProperties
        //        System.err.println ("doMakeRecordFile.setProperties:" + properties);
        recordFile.setProperties(properties);

        return recordFile;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getPathForRecordEntry(Entry entry,
                                        Hashtable requestProperties)
            throws Exception {
        String path = getPathForEntry(null, entry);

        return getPathForRecordEntry(entry, path, requestProperties);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Macro> getMacros(Entry entry) throws Exception {
        List<Macro> macros = null;
        Hashtable   props  = getRecordProperties(entry);
        if (props != null) {
            String m = (String) props.get("requestFields");
            if (m != null) {
                macros = new ArrayList<Macro>();
                for (String macro : StringUtil.split(m, ",", true, true)) {
                    macros.add(
                        new Macro(
                            macro,
                            Utils.getProperty(
                                props, "request." + macro + ".type",
                                "string"), Utils.getProperty(
                                    props, "request." + macro + ".default",
                                    ""), Utils.getProperty(
                                        props, "request." + macro + ".label",
                                        Utils.makeLabel(
                                            macro)), Utils.getProperty(
                                                props, "request." + macro
                                                    + ".values", "")));
                }
            }
        }

        return macros;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param path _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getPathForRecordEntry(Entry entry, String path,
                                        Hashtable requestProperties)
            throws Exception {
        List<Macro> macros = getMacros(entry);
        if (macros != null) {
            for (Macro macro : macros) {

                String value = Utils.getProperty(requestProperties,
                                   "macro_" + macro.name, macro.dflt);
                System.err.println(macro.name + " = " + value);
                path = path.replace("${" + macro.name + "}", value);
            }
        }
        //      System.err.println("Path:" + path);
        if (path.indexOf("${latitude}") >= 0) {
            if (Utils.stringDefined(
                    (String) requestProperties.get("latitude"))) {
                path = path.replace(
                    "${latitude}",
                    (String) requestProperties.get("latitude"));
                path = path.replace(
                    "${longitude}",
                    (String) requestProperties.get("longitude"));
            }
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                path = path.replace("${latitude}", entry.getLatitude() + "");
                path = path.replace("${longitude}",
                                    entry.getLongitude() + "");
            }
            path = path.replace("${latitude}", "40");
            path = path.replace("${longitude}", "-105.2");
        }

        return path;
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFile doMakeRecordFile(Entry entry, Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        String recordFileClass = getTypeProperty("record.file.class",
                                     (String) null);


        if (recordFileClass != null) {
            return doMakeRecordFile(entry, recordFileClass, properties,
                                    requestProperties);
        }



        return (RecordFile) getRecordFileFactory().doMakeRecordFile(
            getPathForRecordEntry(entry, requestProperties), properties,
            requestProperties);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param className _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private RecordFile doMakeRecordFile(Entry entry, String className,
                                        Hashtable properties,
                                        Hashtable requestProperties)
            throws Exception {
        String path = getPathForRecordEntry(entry, requestProperties);
        if (path == null) {
            return null;
        }
        Class c = Misc.findClass(className);
        Constructor ctor = Misc.findConstructor(c, new Class[] { String.class,
                Hashtable.class });
        if (ctor != null) {
            return (RecordFile) ctor.newInstance(new Object[] { path,
                    properties });
        }
        ctor = Misc.findConstructor(c, new Class[] { String.class });

        if (ctor != null) {
            RecordFile recordFile =
                (RecordFile) ctor.newInstance(new Object[] { path });

            return recordFile;
        }

        throw new IllegalArgumentException("Could not find constructor for "
                                           + className);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param recordFile _more_
     * @param filters _more_
     */
    public void getFilters(Request request, Entry entry,
                           RecordFile recordFile,
                           List<RecordFilter> filters) {}

    /**
     * _more_
     *
     * @param path _more_
     * @param filename _more_
     *
     * @return _more_
     */
    @Override
    public boolean canHandleResource(String path, String filename) {
        try {
            boolean ok = getRecordFileFactory().canLoad(path);
            if (ok) {
                return true;
            }
        } catch (Exception exc) {
            //            return false;
        }

        return super.canHandleResource(path, filename);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public RecordFileFactory getRecordFileFactory() {
        if (recordFileFactory == null) {
            recordFileFactory = doMakeRecordFileFactory();
        }

        return recordFileFactory;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public RecordFileFactory doMakeRecordFileFactory() {
        return new RecordFileFactory();
    }


    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean isRecordFile(String path) throws Exception {
        return getRecordFileFactory().canLoad(path);
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
     * @param request _more_
     * @param entry _more_
     * @param services _more_
     *
     */
    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        getRecordOutputHandler().getServiceInfos(request, entry, services);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param icon _more_
     *
     * @return _more_
     */
    public String getAbsoluteIconUrl(Request request, String icon) {
        return request.getAbsoluteUrl(getRepository().getIconUrl(icon));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getChartProperty(Request request, Entry entry, String prop,
                                   String dflt) {
        return getTypeProperty(prop, dflt);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Feb 17, '20
     * @author         Enter your name here...
     */
    public static class Macro {

        /** _more_ */
        String name;

        /** _more_ */
        String dflt;

        /** _more_ */
        String type;

        /** _more_ */
        String label;

        /** _more_ */
        String values;

        /**
         * _more_
         *
         * @param name _more_
         * @param type _more_
         * @param dflt _more_
         * @param label _more_
         * @param values _more_
         */
        public Macro(String name, String type, String dflt, String label,
                     String values) {
            this.name   = name;
            this.type   = type;
            this.dflt   = dflt;
            this.label  = label;
            this.values = values;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return name;
        }
    }

}
