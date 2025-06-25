/**
Copyright (c) 2008-2024 Geode Systems LLC
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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class RecordTypeHandler extends BlobTypeHandler implements RecordConstants,
									   RecordFileContext {
    public static boolean debug = false;
    private static int IDX = 0;
    public static final int IDX_RECORD_COUNT = IDX++;
    public static final int IDX_PROPERTIES = IDX++;
    public static final int IDX_LAST = IDX_PROPERTIES;
    private RecordFileFactory recordFileFactory;
    private RecordOutputHandler recordOutputHandler;

    public RecordTypeHandler(Repository repository, String type,
                             String description) {
        super(repository, type, description);
    }

    public RecordTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    public String getContextNamespace() {
        return getTypeProperty("record.namespace", "record");
    }

    public String getFieldProperty(String field, String key) {
        key = getContextNamespace() + "." + field + "." + key;
        String v = getRepository().getProperty(key);
        if ((v != null) && (v.trim().length() > 0)) {
            return v;
        }

        return null;
    }

    public PropertyProvider getPropertyProvider() {
        return getRepository();
    }

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

    public RecordOutputHandler doMakeRecordOutputHandler() throws Exception {
        return new RecordOutputHandler(getRepository(), null);
    }

    public void addToProcessingForm(Request request, Entry entry,
                                    Appendable sb)
            throws Exception {}

    public boolean includedInRequest(Request request, RecordEntry recordEntry)
            throws Exception {
        return true;
    }

    @Override
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
        Column column = entry.getTypeHandler().getColumn("properties");

	if(column!=null) {
	    int index = column.getOffset();
	    if (values[index] != null) {
		values[index] = "\n" + contents;
	    } else {
		values[index] = contents;
	    }
        }
    }

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

    public void log(String msg) {
        getRepository().getLogManager().logInfo("RecordTypeHandler:" + msg);
    }

    public String getEntryCategory(Entry entry) {
        return getTypeProperty("entry.category", "");
    }

    @Override
    public void initializeCopiedEntry(Entry entry, Entry originalEntry)
            throws Exception {
        super.initializeCopiedEntry(entry, originalEntry);
        initializeNewEntry(null, entry, NewType.COPY);
    }

    public Hashtable getRecordProperties(Entry entry) throws Exception {
        Object[]  values = entry.getTypeHandler().getEntryValues(entry);
	//Some of the types have an empty values array?
	if(values.length==0) return null;
        String    propertiesString = entry.getStringValue(getRepository().getAdminRequest(),
							  "properties","");
	//(values[IDX_PROPERTIES] != null)                                     ? values[IDX_PROPERTIES].toString()                                     : "";

        String    typeProperties   = getRecordPropertiesFromType(null,entry);

        Hashtable p                = null;

        if (typeProperties != null) {
            if (p == null) {
                p = new Hashtable();
            }
            p.putAll(Utils.getProperties(typeProperties,true));
        }

        if (propertiesString != null) {
            if (p == null) {
                p = new Hashtable();
            }
            p.putAll(Utils.getProperties(propertiesString));
        }

        return p;
    }

    public String getRecordPropertiesFromType(Request request,Entry entry) throws Exception {
	String props = getTypeProperty("record.properties", (String) null);
	if(props!=null) {
	    StringBuilder sb = new StringBuilder();
	    boolean continued = false;
	    for(String line: Utils.split(props,"\n")) {

		if(!continued)  sb.append("\n");
		continued=false;
		String tline = line.trim();
		if(tline.trim().endsWith("\\")) {
		    tline=tline.substring(0,tline.length()-1);
		    continued= true;
		}
		sb.append(tline);
	    }
	    props = sb.toString();
	}
        return props;
    }

    public boolean okToCacheRecordFile(Request request, Entry entry) {
        return getTypeProperty("record.file.cacheok", true);
    }

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

	String md5="";
	HashSet<String> exceptArgs = Utils.makeHashSet(ARG_MAX,ARG_LIMIT,ARG_OUTPUT,ARG_PRODUCT,ARG_ENTRYID);
	String args = request.getUrlArgs(exceptArgs);
	if(args.length()>0) {
	    md5 = Utils.makeMD5(args)+"_";
	}

        return "record_" + entry.getChangeDate() + md5 + suffix.toLowerCase() + ".csv";
    }

    public final RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        Hashtable properties = getRecordProperties(entry);
	if(entry.hasLocationDefined(request)) {
	    properties.put("latitude",""+entry.getLatitude(request));
	    properties.put("longitude",""+entry.getLongitude(request));
	} else if(entry.hasAreaDefined(request)) {
	    Rectangle2D.Double bounds = entry.getBounds(request);
	    properties.put("latitude",""+bounds.getCenterY());
	    properties.put("longitude",""+bounds.getCenterX());
	}

        RecordFile recordFile = doMakeRecordFile(request, entry, properties,
                                    request.getDefinedProperties());
        if (recordFile == null) {
            return null;
        }

        return initRecordFile(request, entry, properties, recordFile);
    }

    protected RecordFile initRecordFile(Request request, Entry entry,
                                        Hashtable properties,
                                        RecordFile recordFile)
            throws Exception {
        if (okToCacheRecordFile(request, entry)) {
            String filename = getCacheFileName(request, entry);
	    File file = getRepository().getEntryManager().getCacheFile(entry,filename);
            recordFile.setCacheFile(file);
        } else {
	    //	    System.err.println("initRecordFile: no cache file:" + entry);
	}

        //Explicitly set the properties to force a call to initProperties
	//	System.err.println ("doMakeRecordFile.setProperties:" + properties);
        recordFile.setProperties(properties);

        return recordFile;
    }

    public IO.Path getPathForRecordEntry(Request request,Entry entry,  Hashtable requestProperties)
	throws Exception {
        String thePath = getPathForEntry(request, entry,true);
	if(!stringDefined(thePath)) {
	    throw new IllegalArgumentException("No file specified:" + entry.getName());
	}
        thePath  = convertPath(request,entry, thePath, requestProperties);
	thePath = getRepository().applyPropertyMacros(thePath);
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

	int arg=1;
	while(true) {
	    String property = getTypeProperty("request_arg" + arg,null);
	    if(property==null) break;
 	    arg++;
	    List<String> toks = Utils.splitUpTo(property,":",2);
	    if(toks.size()!=2) continue;
	    String value = getRepository().applyPropertyMacros(toks.get(1));
	    //	    System.err.println("request arg:" + toks.get(0) +"=" + value);
	    path.setRequestArgs(new String[]{toks.get(0),value});
	}

        if (debug) {
            System.err.println(
                "RecordTypeHandler.getPathForRecordEntry entry:" + entry
                + " path:" + thePath + " resource:" + entry.getResource());
        }
        return path;
    }

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

    public String convertPath(Request request, Entry entry, String path,
			      Hashtable requestProperties)
            throws Exception {

        if (debug) {
            System.err.println(
                "RecordTypeHandler.convertPath entry:" + entry
                + " path:" + path);
        }
	if(path==null) return null;
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
            if (entry.hasLocationDefined(request) || entry.hasAreaDefined(request)) {
                path = path.replace("${latitude}", entry.getLatitude(request) + "");
                path = path.replace("${longitude}",
                                    entry.getLongitude(request) + "");
            }
            path = path.replace("${latitude}", "40");
            path = path.replace("${longitude}", "-105.2");
        }
        if (path.indexOf("${north}") >= 0) {
            path = path.replace("${north}", entry.getNorth(request) + "");
            path = path.replace("${west}", entry.getWest(request) + "");
            path = path.replace("${south}", entry.getSouth(request) + "");
            path = path.replace("${east}", entry.getEast(request) + "");

        }

        //      System.err.println(path);

        return path;
    }

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
								    getPathForRecordEntry(request, entry, requestProperties).getPath(), properties,
            requestProperties);
    }

    public RecordFile doMakeRecordFile(Entry entry, String className,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        IO.Path path = getPathForRecordEntry(null,entry, requestProperties);
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

    public void getFilters(Request request, Entry entry,
                           RecordFile recordFile,
                           List<RecordFilter> filters) {}

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

    public RecordFileFactory getRecordFileFactory() {
        if (recordFileFactory == null) {
            recordFileFactory = doMakeRecordFileFactory();
        }

        return recordFileFactory;
    }

    public RecordFileFactory doMakeRecordFileFactory() {
        return new RecordFileFactory();
    }

    public boolean isRecordFile(String path) throws Exception {
        return getRecordFileFactory().canLoad(path);
    }

    public String macro(String s) {
        return "${" + s + "}";
    }

    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        getRecordOutputHandler().getServiceInfos(request, entry, services);
    }

    public boolean shouldProcessResource(Request request, Entry entry) {
	if(entry.getResource().hasResource()) 
	    return entry.getTypeHandler().getTypeProperty("record.processresource",true);
	return entry.getTypeHandler().getTypeProperty("record.processresource",false);
    }

    public String getAbsoluteIconUrl(Request request, String icon) {
        return request.getAbsoluteUrl(getRepository().getIconUrl(icon));
    }

    public String getChartProperty(Request request, Entry entry, String prop,
                                   String dflt) {
        return getTypeProperty(prop, dflt);
    }

    public static class Macro {

        String name;

        String dflt;

        String type;

        String label;

        String values;

        boolean multiple = false;

        String delimiter;

        String template;

        String multitemplate;

        String nonetemplate;

        String rows;

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

        public String toString() {
            return name;
        }
    }

}
