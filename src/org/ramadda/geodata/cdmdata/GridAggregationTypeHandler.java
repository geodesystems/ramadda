/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;

import java.io.File;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Class for handling grid aggregation
 *
 */
@SuppressWarnings("unchecked")
public class GridAggregationTypeHandler extends ExtensibleGroupTypeHandler {

    /** Type index for GUI */
    public static final int INDEX_TYPE = 0;

    /** Coordinate index for GUI */
    public static final int INDEX_COORDINATE = 1;

    /** Fields index for GUI */
    public static final int INDEX_FIELDS = 2;

    /** Files index for GUI */
    public static final int INDEX_FILES = 3;

    /** Pattern index for GUI */
    public static final int INDEX_PATTERN = 4;

    /** Flag for recursing directories */
    public static final int INDEX_RECURSE = 5;


    /** Ingest files index for GUI */
    public static final int INDEX_INGEST = 6;

    /** Add short metadata index for GUI */
    public static final int INDEX_ADDSHORTMETADATA = 7;

    /** Add full metadata index for GUI */
    public static final int INDEX_ADDFULLMETADATA = 8;

    /** GridAggregation type */
    public static final String TYPE_GRIDAGGREGATION = "gridaggregation";


    /**
     * Construct a new GridAggregationTypeHandler
     *
     * @param repository   the Repository
     * @param node         the defining Element
     * @throws Exception   problems
     */
    public GridAggregationTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        //We don't need to do this since the Repository loads in the harvesters based on 
        //plugin classes
        //        getRepository().getHarvesterManager().addHarvesterType(
        //            GridAggregationHarvester.class);
    }



    /**
     * Get the NcML file
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param timestamp  the timestamp
     *
     * @return the file
     *
     * @throws Exception problems getting file
     */
    public File getNcmlFile(Request request, Entry entry, long[] timestamp)
            throws Exception {
        if (request == null) {
            request = getRepository().getTmpRequest();
        }
        String ncml = getNcmlString(request, entry, timestamp);
        if (ncml.length() != 0) {
            String ncmlFileName = entry.getId() + "_" + timestamp[0]
                                  + ".ncml";
            //Use the timestamp from the files to make the ncml file name based on the input files
            File tmpFile = getStorageManager().getScratchFile(ncmlFileName);
            //File tmpFile =
            //  getRepository().getStorageManager().getTmpFile(request, "grid.ncml");
            if ( !tmpFile.exists()) {
                System.err.println("writing new ncml file:" + tmpFile);
                IOUtil.writeFile(tmpFile, ncml);
            } else {
                System.err.println("using existing ncml file:" + tmpFile);
            }

            return tmpFile;
        } else {
            return null;
        }
    }


    /**
     * Do the final initialization
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param fromImport _more_
     */
    @Override
    public void doFinalEntryInitialization(Request request, Entry entry,
                                           boolean fromImport) {
        //Call this to force an initial ingest
        try {
            if (getIngest(entry)) {
                getNcmlString(request, entry, new long[] { 0 });
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        super.doFinalEntryInitialization(request, entry, fromImport);
    }


    /**
     * Get whether we should ingest files
     *
     * @param entry  the Entry
     *
     * @return  true if should ingest
     */
    private boolean getIngest(Entry entry) {
        return Misc.equals(entry.getStringValue(INDEX_INGEST, ""), "true");
    }

    /**
     * Get the recurse property
     *
     * @param entry  the Entry
     *
     * @return true if we should recurse
     */
    private boolean getRecurse(Entry entry) {
        //Object[] values = entry.getValues();
        return Misc.equals(entry.getStringValue(INDEX_RECURSE, ""), "true");
    }

    /**
     * Get the fields values
     *
     * @param entry  the Entry
     *
     * @return  the list of variables
     */
    private List<String> getFields(Entry entry) {
        String       fieldString = entry.getStringValue(INDEX_FIELDS, "");
        List<String> fields      = new ArrayList<String>();
        List<String> lines = StringUtil.split(fieldString, "/", true, true);
        for (String line : lines) {
            List<String> words = StringUtil.split(line, ",", true, true);
            for (String word : words) {
                fields.add(word);
            }
        }

        return fields;
    }


    /**
     * Get the NcML as a String
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param timestamp the timestamp
     *
     * @return String containing the NcML with the NcML of its childrens
     *
     * @throws Exception  problems generating NcML
     */
    private String getNcmlString(Request request, Entry entry,
                                 long[] timestamp)
            throws Exception {

        if (request == null) {
            request = getRepository().getTmpRequest();
        }
        StringBuilder sb = new StringBuilder();

        NcmlUtil ncmlUtil = new NcmlUtil(entry.getStringValue(INDEX_TYPE,
                                NcmlUtil.AGG_JOINEXISTING));
        String timeCoordinate = entry.getStringValue(INDEX_COORDINATE, "time");
        String       files   = entry.getStringValue(INDEX_FILES, "").trim();
        String       pattern = entry.getStringValue(INDEX_PATTERN, "").trim();
        boolean      ingest  = getIngest(entry);
        boolean      recurse = getRecurse(entry);
        List<String> fields  = getFields(entry);
        final boolean harvestMetadata =
            Misc.equals(entry.getStringValue(INDEX_ADDSHORTMETADATA, ""), "true");
        final boolean harvestFullMetadata =
            Misc.equals(entry.getStringValue(INDEX_ADDFULLMETADATA, ""), "true");



        ncmlUtil.openNcml(sb);
        if (ncmlUtil.isJoinExisting()) {
            sb.append(XmlUtil.openTag(NcmlUtil.TAG_AGGREGATION,
                                      XmlUtil.attrs(new String[] {
                NcmlUtil.ATTR_TYPE, NcmlUtil.AGG_JOINEXISTING,
                NcmlUtil.ATTR_DIMNAME, timeCoordinate,
                NcmlUtil.ATTR_TIMEUNITSCHANGE, "true"
            })));
        } else if (ncmlUtil.isUnion()) {
            sb.append(XmlUtil.openTag(NcmlUtil.TAG_AGGREGATION,
                                      XmlUtil.attrs(new String[] {
                                          NcmlUtil.ATTR_TYPE,
                                          NcmlUtil.AGG_UNION, })));
        } else if (ncmlUtil.isJoinNew()) {
            //TODO here
        } else if (ncmlUtil.isEnsemble()) {
            /* Ensemble is now handled below
              String ensembleDimName = "ens";
              ncmlUtil.addEnsembleVariables(sb, ensembleDimName);
              sb.append(XmlUtil.openTag(NcmlUtil.TAG_AGGREGATION,
                                        XmlUtil.attrs(new String[] {
                                            NcmlUtil.ATTR_DIMNAME,
                                            ensembleDimName,
                                            NcmlUtil.ATTR_TYPE,
                                            NcmlUtil.AGG_JOINNEW })));
              for (String var : fields) {
                  sb.append(XmlUtil.tag(NcmlUtil.TAG_VARIABLEAGG,
                                        XmlUtil.attrs(new String[] {
                                            NcmlUtil.ATTR_NAME,
                                            var })));
              }
            */
        } else {
            throw new IllegalArgumentException("Unknown aggregation type:"
                    + ncmlUtil);
        }

        List<Entry> sortedChillens      = new ArrayList<Entry>();
        boolean     childrenAggregation = false;
        List<Entry> childrenEntries =
            getRepository().getEntryManager().getChildren(request, entry);

        //Check if the user specified any files directly
        if ((files != null) && (files.length() > 0)) {
            if ( !entry.getUser().getAdmin()) {
                throw new IllegalArgumentException(
                    "When using the files list in the grid aggregation you must be an administrator");
            }
            List<Entry>       dummyEntries = new ArrayList<Entry>();
            List<File>        filesToUse   = new ArrayList<File>();
            PatternFileFilter filter       = null;
            if ((pattern != null) && (pattern.length() > 0)) {
                filter = new PatternFileFilter(
                    StringUtil.wildcardToRegexp(pattern));
            }

            for (String f : StringUtil.split(files, "\n", true, true)) {
                File file = new File(f);
                if (file.isDirectory()) {
                    List<File> childFiles = IOUtil.getFiles(new ArrayList(),
                                                file, recurse, filter);
                    for (File child : childFiles) {
                        if (child.isDirectory()) {}
                        else if (child.isFile()) {
                            filesToUse.add(child);
                        }
                    }
                } else {
                    if ( !file.exists()) {
                        //What to do???
                    } else {
                        filesToUse.add(file);
                    }
                }
            }

            for (File dataFile : filesToUse) {
                //Check for access
                getStorageManager().checkLocalFile(dataFile);
                dummyEntries.add(makeDummyEntry(dataFile));
            }

            boolean readOnly = getRepository().isReadOnly();

            if (ingest) {
                //See if we have all of the files
                HashSet seen = new HashSet();
                for (Entry existingEntry : childrenEntries) {
                    seen.add(existingEntry.getFile());
                }
                boolean addedNewOne = false;
                for (File dataFile : filesToUse) {
                    if (seen.contains(dataFile)) {
                        continue;
                    }
                    //If the repository is readonly then don't add the entry to the repository
                    if (readOnly) {
                        //System.err.println("Read only  - making dummy entry " + dataFile);
                        childrenEntries.add(makeDummyEntry(dataFile));

                        continue;
                    }

                    addedNewOne = true;
                    final Request    finalRequest = request;
                    EntryInitializer initializer  = new EntryInitializer() {
                        public void initEntry(Entry entry) {
                            if (harvestMetadata || harvestFullMetadata) {
                                try {
                                    List<Entry> entries =
                                        (List<Entry>) Misc.newList(entry);
                                    getEntryManager().addInitialMetadata(
                                        finalRequest, entries, true,
                                        !harvestFullMetadata);
                                } catch (Exception exc) {
                                    throw new RuntimeException(exc);
                                }
                            }
                        }
                    };
                    //                    System.err.println("Adding file to aggregation:" + dataFile);
                    Request addRequest = new Request(getRepository(),
                                             entry.getUser());
                    Entry newEntry =
                        getEntryManager().addFileEntry(addRequest, dataFile,
						       entry, null, dataFile.getName(), "", entry.getUser(),
                            null, initializer);
                    childrenEntries.add(newEntry);
                }
                if (addedNewOne && (harvestMetadata || harvestFullMetadata)) {
                    getRepository().getExtEditor().setTimeFromChildren(
                        request, entry, childrenEntries);
                    Rectangle2D.Double rect =
                        getEntryUtil().getBounds(request,childrenEntries);
                    if (rect != null) {
                        entry.setBounds(rect);
                    }
                    getEntryManager().updateEntry(request, entry);
                }
            } else {
                childrenEntries = dummyEntries;
            }
        }


        for (Entry child : childrenEntries) {
            if (child.getType().equals(TYPE_GRIDAGGREGATION)) {
                String ncml = getNcmlString(request, child, timestamp);
                //MATIAS:
                if (ncml != null) {
                    //                if (ncml!=""){
                    sb.append(ncml);
                    childrenAggregation = true;
                }

                continue;
            }
            //sortedChillens.add(child.getResource().getPath());
            sortedChillens.add(child);

        }

        if (ncmlUtil.isJoinExisting()) {
	    //Get the sort order. false=> don't check for inherited
	    String sortOrder = (String) entry.getValue("sortorder","");
	    if(stringDefined(sortOrder)) {
		sortedChillens= getEntryUtil().sortEntriesOn(sortedChillens,sortOrder,false);
	    } else {
		sortedChillens = getEntryUtil().sortEntriesOnDate(sortedChillens,
								  false);
	    }
        } else if (ncmlUtil.isEnsemble()) {
            sortedChillens = getEntryUtil().sortEntriesOnName(sortedChillens,
                    false);
            String ensembleDimName = "ens";
            ncmlUtil.addEnsembleVariables(sb, ensembleDimName,
                                          sortedChillens);
            sb.append(XmlUtil.openTag(NcmlUtil.TAG_AGGREGATION,
                                      XmlUtil.attrs(new String[] {
                                          NcmlUtil.ATTR_DIMNAME,
                                          ensembleDimName,
                                          NcmlUtil.ATTR_TYPE,
                                          NcmlUtil.AGG_JOINNEW })));
            for (String var : fields) {
                sb.append(XmlUtil.tag(NcmlUtil.TAG_VARIABLEAGG,
                                      XmlUtil.attrs(new String[] {
                                          NcmlUtil.ATTR_NAME,
                                          var })));
            }
        }
        //        System.err.println("making ncml:");
        timestamp[0] = 0;
        for (Entry child : sortedChillens) {
            //            System.err.println("   file:" + s);
            String s = child.getResource().getPath();
            File   f = new File(s);
            timestamp[0] = timestamp[0] ^ f.lastModified() ^ s.hashCode();
            sb.append(
                XmlUtil.tag(
                    NcmlUtil.TAG_NETCDF,
                    XmlUtil.attrs(
                        NcmlUtil.ATTR_LOCATION,
                        IOUtil.getURL(s, getClass()).toString(),
                        NcmlUtil.ATTR_ENHANCE, "true"), ""));
        }

        sb.append(XmlUtil.closeTag(NcmlUtil.TAG_AGGREGATION));
        sb.append(XmlUtil.closeTag(NcmlUtil.TAG_NETCDF));

        //        System.err.println(sb);

        return sb.toString();


    }



    /**
     * Make a dummy Entry
     *
     * @param dataFile  the datafile
     *
     * @return  the dummy Entry
     *
     * @throws Exception  problem making Entry
     */
    private Entry makeDummyEntry(File dataFile) throws Exception {
        Entry dummyEntry =
            new Entry(getRepository().getTypeHandler(TypeHandler.TYPE_FILE),
                      true, IOUtil.getFileTail(dataFile.toString()));
        dummyEntry.setResource(new Resource(dataFile,
                                            Resource.TYPE_LOCAL_FILE));

        return dummyEntry;
    }

    /**
     * Handle a change to a child entry
     *
     * @param entry  the Entry
     * @param isNew  true if is new child
     *
     * @throws Exception problem handling
     */
    @Override
    public void childEntryChanged(Request request,Entry entry, boolean isNew)
            throws Exception {
        super.childEntryChanged(request,entry, isNew);
        Entry parent = entry.getParentEntry();
        List<Entry> children =
            getEntryManager().getChildren(getRepository().getTmpRequest(),
                                          parent);
        //For good measure
        children.add(entry);
        getEntryManager().setBoundsOnEntry(request,parent, children);
    }


    /**
     * Get the services for this type
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param services the list of services
     */
    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);

        /*
        String url =
            HtmlUtils.url(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                          entry), new String[] {
            ARG_OUTPUT, LidarOutputHandler.OUTPUT_LATLONALTCSV.toString(),
            LidarOutputHandler.ARG_LIDAR_SKIP,
            macro(LidarOutputHandler.ARG_LIDAR_SKIP), ARG_BBOX,
            macro(ARG_BBOX),
        }, false);
        services.add(new Service("pointcloud", "Point Cloud",
                                 request.getAbsoluteUrl(url),
                                 getIconUrl(LidarOutputHandler.ICON_POINTS)));
        */
    }



}
