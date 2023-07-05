/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.data.point.PointFile;
import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordFileContext;
import org.ramadda.data.record.RecordFileFactory;
import org.ramadda.data.record.RecordVisitorGroup;

import org.ramadda.data.record.VisitInfo;

import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.RecordEntry;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.PropertyProvider;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import java.io.File;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public abstract class RecordTypeHandler extends BlobTypeHandler implements RecordConstants,
        RecordFileContext {

    /**  */
    public static boolean debug = false;

    /** _more_ */
    private static int IDX = 0;

    /** _more_ */
    public static final int IDX_RECORD_COUNT = IDX++;

    /** _more_ */
    public static final int IDX_PROPERTIES = IDX++;

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
    public PropertyProvider getPropertyProvider() {
        return getRepository();
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
        if ( !shouldProcessResource(request, entry)) {
            return;
        }

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
        initializeNewEntry(null, entry, false);
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
	//Some of the types have an empty values array?
	if(values.length==0) return null;
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
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean okToCacheRecordFile(Request request, Entry entry) {
        return getTypeProperty("record.file.cacheok", true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getCacheFileName(Request request, Entry entry)
            throws Exception {
        String      suffix = "";
        List<Macro> macros = getMacros(entry);
        if (macros != null) {
            for (Macro macro : macros) {
                String v = request.getString("request." + macro.name,
                                             (macro.dflt != null)
                                             ? macro.dflt
                                             : "");
                v      = v.replaceAll("\\.", "_").replaceAll("/", "_");
                suffix += "_" + v;
            }
        }

        return "record_" + entry.getChangeDate() + suffix + ".csv";
    }


    /**
     * main top level entry point to make the RecordFile
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public final RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        Hashtable properties = getRecordProperties(entry);
        RecordFile recordFile = doMakeRecordFile(request, entry, properties,
                                    request.getDefinedProperties());
        if (recordFile == null) {
            return null;
        }

        return initRecordFile(request, entry, properties, recordFile);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param recordFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected RecordFile initRecordFile(Request request, Entry entry,
                                        Hashtable properties,
                                        RecordFile recordFile)
            throws Exception {
        File file = null;
        if (okToCacheRecordFile(request, entry)) {
            String filename = getCacheFileName(request, entry);
            //      System.err.println("cache file:" + filename);
            file = getRepository().getEntryManager().getCacheFile(entry,
                    filename);
            recordFile.setCacheFile(file);
        }


        //Explicitly set the properties to force a call to initProperties
	//	System.err.println ("doMakeRecordFile.setProperties:" + properties);
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
    public IO.Path getPathForRecordEntry(Entry entry,  Hashtable requestProperties)
	throws Exception {
        String thePath = getPathForEntry(null, entry,true);
        thePath  = convertPath(entry, thePath, requestProperties);
	IO.Path path = new IO.Path(thePath);

	List<Metadata> metadataList =
	    getMetadataManager().findMetadata(getRepository().getTmpRequest(), entry,
					      new String[]{"requestinformation"}, true);
	if ((metadataList != null) && (metadataList.size() > 0)) {
	    Metadata mtd= metadataList.get(0);
	    path.setMethod(mtd.getAttr1());
	    String args = mtd.getAttr2();
	    if(stringDefined(args)) {
		for(String pair: Utils.split(args,"\n",true,true)) {
		    List<String> tuple= Utils.splitUpTo(pair,"=",2);
		    if(tuple.size()==2) {
			String v = getRepository().applyPropertyMacros(tuple.get(1).trim());
			//			System.err.println("request arg:" + tuple.get(0) +" value:" + v);
			path.setRequestArgs(new String[]{tuple.get(0).trim(),v});
		    }
		}
	    }
	    if(stringDefined(mtd.getAttr3())) {
		path.setBody(getRepository().applyPropertyMacros(mtd.getAttr3()));
		//		System.err.println("body:" +getRepository().applyPropertyMacros(mtd.getAttr3()));
	    }
	}

        if (debug) {
            System.err.println(
                "RecordTypeHandler.getPathForRecordEntry entry:" + entry
                + " path:" + thePath + " resource:" + entry.getResource());
        }
        return path;
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
                for (String macro : Utils.split(m, ",", true, true)) {
                    macros.add(new Macro(macro, props));
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
    public String convertPath(Entry entry, String path,
			      Hashtable requestProperties)
            throws Exception {
        if (debug) {
            System.err.println(
                "RecordTypeHandler.convertPath entry:" + entry
                + " path:" + path);
        }
        List<Macro> macros = getMacros(entry);
        if (macros != null) {
            for (Macro macro : macros) {
                Object prop = requestProperties.get("request." + macro.name);
                //              System.err.println("macro:" + macro.name +" prop:" + prop);
                if (prop == null) {
                    prop = (macro.dflt != null)
                           ? macro.dflt
                           : "";
                    if ((macro.dflt != null) && (macro.template != null)) {
                        List<String> dflts = Utils.split(macro.dflt, ",",
                                                 true, true);
                        if (dflts.size() > 1) {
                            List<String> values = new ArrayList<String>();
                            for (String s : dflts) {
                                s = macro.template.replace("${value}", s);
                                values.add(s);
                            }
                            prop = Utils.join(values,
                                    (macro.delimiter != null)
                                    ? macro.delimiter
                                    : "");
                            if (macro.multitemplate != null) {
                                prop = macro.multitemplate.replace(
                                    "${value}", prop.toString());
                            }
                        } else if (dflts.size() == 1) {
                            prop = macro.template.replace("${value}",
                                    prop.toString());
                        }
                    }

                } else {
                    //              System.err.println("value:" + prop);
                }
                String value;
                //Handle lists different?
                if (prop instanceof List) {
                    value = prop.toString();
                } else {
                    value = prop.toString();
                }
                value = value.replaceAll(" ", "%20");
                path  = path.replace("${" + macro.name + "}", value);
                //              System.err.println("path:" + path);
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
        if (path.indexOf("${north}") >= 0) {
            path = path.replace("${north}", entry.getNorth() + "");
            path = path.replace("${west}", entry.getWest() + "");
            path = path.replace("${south}", entry.getSouth() + "");
            path = path.replace("${east}", entry.getEast() + "");

        }

        //      System.err.println(path);

        return path;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        String recordFileClass = getTypeProperty("record.file.class",
                                     (String) null);

        if (recordFileClass != null) {
            return doMakeRecordFile(entry, recordFileClass, properties,
                                    requestProperties);
        }

        return (RecordFile) getRecordFileFactory().doMakeRecordFile(
								    getPathForRecordEntry(entry, requestProperties).getPath(), properties,
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
    public RecordFile doMakeRecordFile(Entry entry, String className,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        IO.Path path = getPathForRecordEntry(entry, requestProperties);
        if (path == null) {
            return null;
        }

        Class c = Misc.findClass(className);
        Constructor ctor = Misc.findConstructor(c, new Class[] { IO.Path.class,
                Hashtable.class });
        if (ctor != null) {
            return (RecordFile) ctor.newInstance(new Object[] { path, properties });
        }
        ctor = Misc.findConstructor(c, new Class[] { IO.Path.class });

        if (ctor != null) {
            RecordFile recordFile =
                (RecordFile) ctor.newInstance(new Object[] {path});

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
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean shouldProcessResource(Request request, Entry entry) {
        return entry.getResource().hasResource() && entry.getTypeHandler().getTypeProperty("record.processresource",true);
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

        /** _more_ */
        boolean multiple = false;

        /** _more_ */
        String delimiter;

        /** _more_ */
        String template;

        /** _more_ */
        String multitemplate;

        /** _more_ */
        String nonetemplate;

        /** _more_ */
        String rows;

        /**
         * _more_
         *
         * @param macro _more_
         * @param props _more_
         */
        public Macro(String macro, Hashtable props) {
            this.name = macro;
            type = Utils.getProperty(props, "request." + macro + ".type",
                                     "string");
            dflt = Utils.getProperty(props, "request." + macro + ".default",
                                     null);
            label = Utils.getProperty(props, "request." + macro + ".label",
                                      Utils.makeLabel(macro));
            values = Utils.getProperty(props, "request." + macro + ".values",
                                       "");
            multiple = Utils.getProperty(props,
                                         "request." + macro + ".multiple",
                                         false);
            delimiter = Utils.getProperty(props,
                                          "request." + macro + ".delimiter",
                                          null);
            template = Utils.getProperty(props,
                                         "request." + macro + ".template",
                                         null);
            multitemplate = Utils.getProperty(props,
                    "request." + macro + ".multitemplate", null);
            nonetemplate = Utils.getProperty(props,
                                             "request." + macro
                                             + ".nonetemplate", null);
            rows = Utils.getProperty(props, "request." + macro + ".rows",
                                     null);
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
