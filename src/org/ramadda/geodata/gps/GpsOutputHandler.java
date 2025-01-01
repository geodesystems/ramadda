/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.gps;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.Utils;
import org.ramadda.util.HtmlUtils;




import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;


import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.*;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class GpsOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String PROP_TEQC = "gps.teqc";

    /** _more_ */
    public static final String PROP_RUNPKR = "gps.runpkr";

    /** _more_ */
    public static final String OPUS_URL =
        "http://www.ngs.noaa.gov/OPUS-cgi/OPUS/prod/upload.prl";

    /** _more_ */
    private static final String TEQC_FLAG_QC = "+qcq";

    /** _more_ */
    private static final String TEQC_UNKNOWN = "-Unknown-";

    /** _more_ */
    private static final String TEQC_FLAG_META = "+meta";


    /** _more_ */
    public static final String OPUS_TITLE = "Add OPUS";

    /** _more_ */
    public static final String URL_ADDOPUS = "/gps/addopus";

    /** _more_ */
    public static final String ARG_COORD_LONLATALT = "coord.lonlatalt";

    /** _more_ */
    public static final String ARG_COORD_XYZ = "coord.xyz";


    /** _more_ */
    public static final String ARG_OPUS = "opus";

    /** _more_ */
    public static final String ARG_RINEX_ID = "rinex.id";

    /** _more_ */
    public static final String ARG_CONTROLPOINTS_COMMENT =
        "controlpoints.comment";

    /** _more_ */
    public static final int IDX_FORMAT = 0;

    /** _more_ */
    public static final int IDX_SITE_CODE = 1;

    /** _more_ */
    public static final int IDX_ANTENNA_TYPE = 2;

    /** _more_ */
    public static final int IDX_ANTENNA_HEIGHT = 3;


    /** _more_ */
    public static final String ASSOCIATION_TYPE_GENERATED_FROM =
        "generated_from";


    /** _more_ */
    private static final String RINEX_SUFFIX = ".rinex";


    /** _more_ */
    private static final String ARG_OPUS_EMAIL = "email_address";

    /** _more_ */
    private static final String ARG_OPUS_ANTENNA = "ant_type";

    /** _more_ */
    private static final String ARG_OPUS_RAPID = "opus.rapid";

    /** _more_ */
    private static final String ARG_OPUS_HEIGHT = "height";


    /** _more_ */
    private static final String ARG_RINEX_PROCESS = "rinex.process";

    /** _more_ */
    private static final String ARG_PROCESS = "gps.process";

    /** _more_ */
    private static final String ARG_OPUS_PROCESS = "opus.process";

    /** _more_ */
    private static final String ARG_RINEX_FILE = "rinex.file";

    /** _more_ */
    private static final String ARG_GPS_FILE = "gps.file";

    /** _more_ */
    private static final String ARG_GPS_SELECTED = "gps.selected";

    /** _more_ */
    private static final String ARG_GPS_ANTENNA_TYPE = "gps.antenna.type";

    /** _more_ */
    private static final String ARG_GPS_ANTENNA_HEIGHT = "gps.antenna.height";

    /** _more_ */
    private static final String ARG_RINEX_DOWNLOAD = "rinex.download";

    /** file path to the teqc executable */
    private String teqcPath;

    /** file path to the teqc executable */
    private String runPkrPath;



    /** The output type */
    public static final OutputType OUTPUT_GPS_TORINEX =
        new OutputType("Convert to RINEX", "gps.torinex",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       "/gps/gps.png", "Field Project");

    /** _more_ */
    public static final OutputType OUTPUT_GPS_BULKEDIT =
        new OutputType("Edit GPS Metadata", "gps.bulkedit",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       "/gps/gps.png", "Field Project");

    /** _more_ */
    public static final OutputType OUTPUT_GPS_METADATA =
        new OutputType("Show GPS Metadata", "gps.metadata",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       "/gps/gps.png", "Field Project");

    /** _more_ */
    public static final OutputType OUTPUT_GPS_QC =
        new OutputType("Show GPS QC", "gps.qc", OutputType.TYPE_OTHER,
                       OutputType.SUFFIX_NONE, "/gps/gps.png",
                       "Field Project");

    /** _more_ */
    public static final OutputType OUTPUT_GPS_OPUS =
        new OutputType("Submit to OPUS", "gps.gps.opus",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       "/gps/opus.png", "Field Project");

    /** _more_ */
    public static final OutputType OUTPUT_GPS_CONTROLPOINTS =
        new OutputType("Make Control Points", "gps.gps.controlpoints",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       "/icons/csv.png", "Field Project");

    /**
     * ctor
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public GpsOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GPS_TORINEX);
        addType(OUTPUT_GPS_BULKEDIT);

        addType(OUTPUT_GPS_METADATA);
        addType(OUTPUT_GPS_QC);
        addType(OUTPUT_GPS_OPUS);
        addType(OUTPUT_GPS_CONTROLPOINTS);
        teqcPath   = getRepository().getScriptPath(PROP_TEQC);
        runPkrPath = getRepository().getScriptPath(PROP_RUNPKR);
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private boolean isRawGps(Entry entry) {
        return entry.getTypeHandler().isType(GpsTypeHandler.TYPE_RAW);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private boolean isGps(Entry entry) {
        return entry.getTypeHandler().isType(GpsTypeHandler.TYPE_GPS);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private boolean isSolution(Entry entry) {
        return entry.getTypeHandler().isType(
            SolutionTypeHandler.TYPE_SOLUTION);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private boolean isRinex(Entry entry) {
        return entry.getTypeHandler().isType(GpsTypeHandler.TYPE_RINEX);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveTeqc() {
        return teqcPath != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveRunPkr() {
        return runPkrPath != null;
    }


    /**
     * This method gets called to determine if the given entry or entries can be displays as las xml
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.group != null) {
            List<Entry> entries = state.getAllEntries();
            if (haveTeqc()) {
                for (Entry child : entries) {
                    if (isRawGps(child)) {
                        links.add(makeLink(request, state.group,
                                           OUTPUT_GPS_TORINEX));

                        break;
                    }
                }
            }

            for (Entry child : entries) {
                if (isGps(child)) {
                    if (getAccessManager().canDoEdit(request, child)) {
                        links.add(makeLink(request, state.group,
                                           OUTPUT_GPS_BULKEDIT));

                        break;
                    }
                }
            }
            for (Entry child : entries) {
                if (isRinex(child)) {
                    links.add(makeLink(request, state.group,
                                       OUTPUT_GPS_OPUS));

                    break;
                }
            }
            for (Entry child : entries) {
                if (isSolution(child)) {
                    links.add(makeLink(request, state.group,
                                       OUTPUT_GPS_CONTROLPOINTS));

                    break;
                }
            }
        } else if (state.entry != null) {
            if (isRawGps(state.entry)) {
                links.add(makeLink(request, state.entry,
                                   OUTPUT_GPS_METADATA));
                links.add(makeLink(request, state.entry, OUTPUT_GPS_TORINEX));
            }
            if (isRinex(state.entry)) {
                links.add(makeLink(request, state.entry,
                                   OUTPUT_GPS_METADATA));
                links.add(makeLink(request, state.entry, OUTPUT_GPS_QC));
                links.add(makeLink(request, state.entry, OUTPUT_GPS_OPUS));

            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        if (outputType.equals(OUTPUT_GPS_TORINEX)) {
            return outputRinex(request, group, children);
        }
        if (outputType.equals(OUTPUT_GPS_BULKEDIT)) {
            return outputBulkEdit(request, group, children);
        }
        if (outputType.equals(OUTPUT_GPS_CONTROLPOINTS)) {
            return outputControlpoint(request, group, children);
        }

        return outputOpus(request, group, children);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        if (outputType.equals(OUTPUT_GPS_TORINEX)) {
            return outputRinex(request, entry, entries);
        }
        if (outputType.equals(OUTPUT_GPS_METADATA)) {
            return outputMetadata(request, entry);
        }
        if (outputType.equals(OUTPUT_GPS_QC)) {
            return outputQC(request, entry);
        }

        return outputOpus(request, entry, entries);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result zipResults(Request request, Entry mainEntry)
            throws Exception {

        request.setReturnFilename("rinex.zip");
        OutputStream os = request.getHttpServletResponse().getOutputStream();
        request.getHttpServletResponse().setContentType("application/zip");
        ZipOutputStream zos = new ZipOutputStream(os);
        File[] files = getWorkDir(request.getString(ARG_RINEX_DOWNLOAD,
                           "bad")).listFiles();
        for (File f : files) {
            if ( !f.getName().endsWith(RINEX_SUFFIX) || (f.length() == 0)) {
                continue;
            }
            zos.putNextEntry(new ZipEntry(f.getName()));
            InputStream fis =
                getStorageManager().getFileInputStream(f.toString());
            IOUtil.writeTo(fis, zos);
            IOUtil.close(fis);
        }
        IOUtil.close(zos);
        Result result = new Result();
        result.setNeedToWrite(false);

        return result;
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
    private Result outputMetadata(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtils.formTable());
        int cnt = 0;
        for (String line :
                StringUtil.split(extractGpsMetadata(entry.getFile(),
                    TEQC_FLAG_META), "\n", true, true)) {
            cnt++;
            //skip the filename
            if (cnt == 1) {
                continue;
            }
            List<String> toks = StringUtil.splitUpTo(line, ":", 2);
            if (toks.size() < 2) {
                continue;
            }
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel(StringUtil.camelCase(toks.get(0))),
                    toks.get(1)));
            sb.append("</tr>");
        }
        sb.append(HtmlUtils.formTableClose());

        return new Result("GPS Metadata", sb);
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
    private Result outputQC(Request request, Entry entry) throws Exception {
        StringBuffer sb            = new StringBuffer();
        int          cnt           = 0;
        int          STATE_START   = 0;
        int          STATE_PLOT    = 1;
        int          STATE_PRELIST = 2;
        int          STATE_LIST    = 3;
        int          state         = STATE_START;

        for (String line :
                StringUtil.split(extractGpsMetadata(entry.getFile(),
                    TEQC_FLAG_QC), "\n", false, false)) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            if (state == STATE_START) {
                if (trimmed.startsWith("version:")) {
                    state = STATE_PLOT;
                    sb.append("<pre>");
                }

                continue;
            }
            if (state == STATE_PLOT) {
                if (trimmed.startsWith("*******")) {
                    state = STATE_PRELIST;
                    sb.append("</pre>");
                    sb.append(HtmlUtils.formTable());
                } else {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            if (state == STATE_PRELIST) {
                if (trimmed.startsWith("*******")) {
                    state = STATE_LIST;
                }

                continue;
            }
            if (state == STATE_LIST) {
                List<String> toks = StringUtil.splitUpTo(line, ":", 2);
                if (toks.size() < 2) {
                    continue;
                }
                sb.append(
                    HtmlUtils.formEntry(
                        msgLabel(
                            toks.get(0).replaceAll("<", "&lt").replaceAll(
                                ">", "&gt;")), toks.get(1)));
            }
        }
        sb.append(HtmlUtils.formTableClose());

        return new Result("GPS Metadata", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputRinex(Request request, Entry mainEntry,
                               List<Entry> entries)
            throws Exception {

        if (request.exists(ARG_RINEX_DOWNLOAD)) {
            return zipResults(request, mainEntry);
        }


        StringBuffer sb = new StringBuffer();
        if ( !request.get(ARG_RINEX_PROCESS, false)) {
            sb.append(HtmlUtils.p());
            sb.append(request.form(getRepository().URL_ENTRY_SHOW));
            sb.append(HtmlUtils.hidden(ARG_OUTPUT,
                                       OUTPUT_GPS_TORINEX.getId()));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, mainEntry.getId()));
            sb.append(HtmlUtils.hidden(ARG_RINEX_PROCESS, "true"));

            if (entries.size() == 1) {
                sb.append(HtmlUtils.hidden(ARG_GPS_FILE,
                                           entries.get(0).getId()));
            } else {
                sb.append(msgHeader("Select entries"));
                for (Entry entry : entries) {
                    if ( !isRawGps(entry)) {
                        continue;
                    }
                    boolean hasRinex =
                        getEntryUtil()
                            .getEntriesWithType(getAssociationManager()
                                .getTailEntriesWithAssociationType(request,
                                    entry,
                                    GpsOutputHandler
                                        .ASSOCIATION_TYPE_GENERATED_FROM), GpsTypeHandler
                                            .TYPE_RINEX).size() > 0;

                    if (request.isAnonymous()) {
                        hasRinex = false;
                    }
                    sb.append(HtmlUtils.checkbox(ARG_GPS_FILE, entry.getId(),
                            !hasRinex));
                    sb.append(" ");
                    sb.append(entry.getName());
                    if (hasRinex) {
                        sb.append(" ");
                        sb.append(
                            HtmlUtils.italics("Already has a RINEX file"));
                    }
                    sb.append(HtmlUtils.br());
                }
            }


            sb.append(HtmlUtils.p());
            sb.append(HtmlUtils.formTable());
            addPublishWidget(
                request, mainEntry, sb,
                msg("Optionally select a folder to publish the RINEX to"),
                false);
            sb.append(HtmlUtils.formTableClose());

            sb.append(HtmlUtils.submit("Make RINEX"));
            sb.append(HtmlUtils.formClose());

            return new Result("", sb);
        }


        List<String> entryIds = request.get(ARG_GPS_FILE,
                                            new ArrayList<String>());



        boolean anyOK = false;
        sb.append(msgHeader("Results"));
        sb.append("<ul>");
        Object uniqueId = getRepository().getGUID();
        File   workDir  = getWorkDir(uniqueId);

        Hashtable<String, Entry> fileToEntryMap = new Hashtable<String,
                                                      Entry>();

        HashSet<File> rinexFiles = new HashSet<File>();
        for (String entryId : entryIds) {
            Entry rawEntry = getEntryManager().getEntry(request, entryId);
            if (rawEntry == null) {
                throw new IllegalArgumentException("No entry:" + entryId);
            }

            if ( !isRawGps(rawEntry)) {
                sb.append("<li>");
                sb.append("Skipping:" + rawEntry.getName()
                          + " because its not a raw GPS file");
                sb.append(HtmlUtils.p());

                continue;
            }

            File rawFile = rawEntry.getFile();
            if ( !rawFile.exists()) {
                throw new IllegalStateException("File does not exist:"
                        + rawFile);
            }

            String inputFile = getRawFile(rawFile.toString(), sb);
            if (inputFile == null) {
                sb.append("<li>");
                sb.append(
                    "Skipping:" + rawEntry.getName()
                    + " because it is trimble and runpkr is not installed");
                sb.append(HtmlUtils.p());

                continue;
            }

            if ( !new File(inputFile).exists()) {
                sb.append("<li>");
                sb.append("Could not find raw file for "
                          + rawEntry.getName());
                sb.append(HtmlUtils.p());

                continue;
            }

            GregorianCalendar cal =
                new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
            cal.setTime(new Date(rawEntry.getStartDate()));

            String tail = IOUtil.stripExtension(
                              getStorageManager().getFileTail(rawEntry));
            tail = tail + "_" + cal.get(cal.YEAR) + "_"
                   + StringUtil.padLeft("" + cal.get(cal.MONTH), 2, "0")
                   + "_"
                   + StringUtil.padLeft("" + cal.get(cal.DAY_OF_MONTH), 2,
                                        "0");
            tail = tail + RINEX_SUFFIX;

            File rinexFile = new File(IOUtil.joinDir(workDir, tail));

            int  cnt       = 0;
            while (fileToEntryMap.get(rinexFile.toString()) != null) {
                cnt++;
                if (cnt == 1) {
                    sb.append("Note:" + rawEntry.getName()
                              + " is a duplicate file.");
                }
                rinexFile = new File(IOUtil.joinDir(workDir,
                        "copy_" + cnt + "_" + tail));
            }
            //            System.err.println ("rinex file:" + rinexFile.toString());
            fileToEntryMap.put(rinexFile.toString(), rawEntry);
            List<String> args = new ArrayList<String>();
            args.add(teqcPath);
            String antenna = rawEntry.getStringValue(request,IDX_ANTENNA_TYPE,
                                 (String) null);
            double height = rawEntry.getDoubleValue(request,IDX_ANTENNA_HEIGHT, 0.0);

            if (height != 0) {
                args.add("-O.pe");
                args.add("" + height);
                args.add("0");
                args.add("0");
            }
            if ((antenna != null) && (antenna.length() > 0)
                    && !antenna.equalsIgnoreCase(Antenna.NONE)) {
                args.add("-O.at");
                args.add(antenna);
            }
            args.add("+out");
            args.add(rinexFile.toString());
            args.add(inputFile);
            System.err.println("work dir:" + workDir + " rinex file:"
                               + rinexFile);
            System.err.println("args:" + args);
            JobManager.CommandResults results =
                getRepository().getJobManager().executeCommand(args, workDir);
            String errorMsg = results.getStderrMsg();
            String outMsg   = results.getStdoutMsg();
            sb.append("<li>");
            sb.append(rawEntry.getName());

            if (rinexFile.exists()) {
                if (rinexFile.length() == 0) {
                    sb.append(" ... Error: Zero length RINEX file created. ");
                } else {
                    if (errorMsg.length() > 0) {
                        sb.append(" ... RINEX file generated with warnings:");
                    } else {
                        sb.append(" ... RINEX file generated");
                    }
                    anyOK = true;
                }
            } else {
                sb.append(" ... Error:");
            }
            if ((errorMsg.length() > 0) || (outMsg.length() > 0)) {
                sb.append(
                    "<pre style=\"  border: solid 1px #000; max-height: 150px;overflow-y: auto; \">");
                sb.append(errorMsg);
                sb.append(outMsg);
                sb.append("</pre>");
            }
        }

        sb.append("</ul>");
        if ( !anyOK) {
            return new Result("", sb);
        }

        if (doingPublish(request)) {
            Entry parent = getEntryManager().findGroup(request,
                               request.getString(ARG_PUBLISH_ENTRY
                                   + "_hidden", ""));
            if (parent == null) {
                throw new IllegalArgumentException("Could not find folder");
            }
            if ( !getAccessManager().canDoNew(request, parent)) {
                throw new AccessException("No access", request);
            }
            File[] files = workDir.listFiles();
            int    cnt   = 0;
            for (File f : files) {
                String originalFileLocation = f.toString();
                Entry  rawEntry = fileToEntryMap.get(originalFileLocation);
                System.err.println("file from product dir:" + f);
                if ( !f.getName().endsWith(RINEX_SUFFIX)
                        || (f.length() == 0)) {
                    System.err.println("skipping:" + f);

                    continue;
                }
                //Get the name first
                String name = f.getName();

                //Copy the tmp file to storage. Use the storage name 
                f = getStorageManager().copyToStorage(request, f,
                        getStorageManager().getStorageFileName(f.getName()));

                TypeHandler typeHandler =
                    getRepository().getTypeHandler(GpsTypeHandler.TYPE_RINEX);
                Object[] tmpValues = null;
                if (rawEntry.getValues() != null) {
                    tmpValues = (Object[]) rawEntry.getValues().clone();
                    tmpValues[IDX_FORMAT] = "RINEX";
                }
                final Object[]   values      = tmpValues;
                EntryInitializer initializer = new EntryInitializer() {
                    public void initEntry(Entry entry) {
                        entry.setValues(values);
                    }
                };

                Entry newEntry = getEntryManager().addFileEntry(request, f,
								parent, null, name, "",request.getUser(),
                                     typeHandler, initializer);

                if (cnt == 0) {
                    sb.append(msgHeader("Published Entries"));
                }
                cnt++;
                sb.append(
                    HtmlUtils.href(
                        HtmlUtils.url(
                            getRepository().URL_ENTRY_SHOW.toString(),
                            new String[] { ARG_ENTRYID,
                                           newEntry.getId() }), newEntry
                                           .getName()));

                sb.append("<br>");
                getAuthManager().addAuthToken(request);
                getAssociationManager().addAssociation(request, newEntry,
                        rawEntry, "generated rinex",
                        ASSOCIATION_TYPE_GENERATED_FROM);
            }
        }

        sb.append(HtmlUtils.p());
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_GPS_TORINEX.getId()));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, mainEntry.getId()));
        sb.append(HtmlUtils.hidden(ARG_RINEX_DOWNLOAD, uniqueId));
        sb.append(HtmlUtils.submit("Download Results"));
        sb.append(HtmlUtils.formClose());

        return new Result("", sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputBulkEdit(Request request, Entry mainEntry,
                                  List<Entry> entries)
            throws Exception {

        HashSet      selected = null;
        HashSet      changed  = new HashSet();
        StringBuffer sb       = new StringBuffer();
        if (request.get(ARG_RINEX_PROCESS, false)) {
            selected = new HashSet();
            int    cnt             = 0;
            Double overrideHeight  = null;
            String overrideAntenna = null;
            if (request.defined(ARG_GPS_ANTENNA_TYPE)
                    && !request.getString(
                        ARG_GPS_ANTENNA_TYPE, "").trim().equals(
                        Antenna.NONE.trim())) {
                overrideAntenna = request.getString(ARG_GPS_ANTENNA_TYPE, "");
            }
            if (request.defined(ARG_GPS_ANTENNA_HEIGHT)) {
                overrideHeight =
                    Double.valueOf(request.get(ARG_GPS_ANTENNA_HEIGHT, 0.0));
            }

            while (true) {
                cnt++;
                String suffix = "_" + cnt;
                if ( !request.defined(ARG_GPS_FILE + suffix)) {
                    break;
                }
                String entryId = request.getString(ARG_GPS_FILE + suffix, "");
                Entry  entry   = getEntryManager().getEntry(request, entryId);
                if (entry == null) {
                    throw new IllegalArgumentException("No entry:" + entryId);
                }

                if ( !isGps(entry)) {
                    sb.append(HtmlUtils.p());

                    continue;
                }
                if ( !request.get(ARG_GPS_SELECTED + suffix, false)) {
                    continue;
                }
                if ( !getAccessManager().canDoEdit(request, entry)) {
                    continue;
                }

                if ( !getAccessManager().canDoEdit(request, entry)) {
                    throw new AccessException("Cannot edit:"
                            + entry.getLabel(), request);
                }
                Object[] values =
                    ((GenericTypeHandler) entry.getTypeHandler())
                        .getEntryValues(entry);
                Object oldAntenna = values[IDX_ANTENNA_TYPE];
                Object oldHeight  = values[IDX_ANTENNA_HEIGHT];
                selected.add(entry.getId());
                if (overrideAntenna != null) {
                    values[IDX_ANTENNA_TYPE] = overrideAntenna;
                } else if (request.defined(ARG_GPS_ANTENNA_TYPE + suffix)) {
                    values[IDX_ANTENNA_TYPE] =
                        request.getString(ARG_GPS_ANTENNA_TYPE + suffix, "");
                }
                if (overrideHeight != null) {
                    values[IDX_ANTENNA_HEIGHT] = overrideHeight;
                } else if (request.defined(ARG_GPS_ANTENNA_HEIGHT + suffix)) {
                    values[IDX_ANTENNA_HEIGHT] =
                        Double.valueOf(request.get(ARG_GPS_ANTENNA_HEIGHT
                            + suffix, 0.0));
                }
                entry.setValues(values);
                if ( !Misc.equals(oldAntenna, values[IDX_ANTENNA_TYPE])
                        || !Misc.equals(oldHeight,
                                        values[IDX_ANTENNA_HEIGHT])) {
                    changed.add(entry.getId());
                    getEntryManager().updateEntry(request, entry);
                }
            }
            if (changed.size() > 0) {
                sb.append(getPageHandler().showDialogNote(changed.size()
                        + " " + msg("entries have been updated")));
            } else if (selected.size() > 0) {
                sb.append(
                    getPageHandler().showDialogNote(
                        msg("No entries were changed")));
            }
        }

        sb.append(HtmlUtils.p());
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_GPS_BULKEDIT.getId()));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, mainEntry.getId()));
        sb.append(HtmlUtils.hidden(ARG_RINEX_PROCESS, "true"));


        sb.append(msgHeader("Entries to edit"));
        sb.append(HtmlUtils.formTable());
        int cnt = 0;
        for (Entry entry : entries) {
            if ( !isGps(entry)) {
                continue;
            }
            if ( !getAccessManager().canDoEdit(request, entry)) {
                continue;
            }

            if (cnt == 0) {
                sb.append(HtmlUtils.row(HtmlUtils.cols("", "",
                        msg("GPS File"), msg("Antenna Height (meters)"),
                        msg("Antenna"))));
            }
            cnt++;
            String suffix = "_" + cnt;
            sb.append(HtmlUtils.hidden(ARG_GPS_FILE + suffix, entry.getId()));
            boolean entrySelected = true;
            if (selected != null) {
                entrySelected = selected.contains(entry.getId());
            }
            sb.append(
                HtmlUtils.row(HtmlUtils.cols(changed.contains(entry.getId())
                                             ? "Changed"
                                             : "", HtmlUtils.checkbox(
                                             ARG_GPS_SELECTED
                                             + suffix, "true", entrySelected), entry.getName(), HtmlUtils.input(
                                                 ARG_GPS_ANTENNA_HEIGHT
                                                     + suffix, entry.getStringValue(request,
                                                         IDX_ANTENNA_HEIGHT, ""), 5), HtmlUtils.select(
                                                             ARG_GPS_ANTENNA_TYPE
                                                                 + suffix, Antenna.getAntennas(), entry.getStringValue(request,
                                                                     IDX_ANTENNA_TYPE, "")))));

        }

        sb.append(HtmlUtils.row(HtmlUtils.cols("", "",
                msgLabel("Override values"),
                HtmlUtils.input(ARG_GPS_ANTENNA_HEIGHT, "", 5),
                HtmlUtils.select(ARG_GPS_ANTENNA_TYPE, Antenna.getAntennas(),
                                 ""))));


        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit("Apply Edits"));
        sb.append(HtmlUtils.formClose());

        return new Result("", sb);


    }




    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputControlpoint(Request request, Entry mainEntry,
                                      List<Entry> entries)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        if ( !request.get(ARG_PROCESS, false)) {
            sb.append(HtmlUtils.p());
            sb.append(request.form(getRepository().URL_ENTRY_SHOW));
            sb.append(HtmlUtils.hidden(ARG_OUTPUT,
                                       OUTPUT_GPS_CONTROLPOINTS.getId()));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, mainEntry.getId()));
            sb.append(HtmlUtils.hidden(ARG_PROCESS, "true"));
            sb.append(HtmlUtils.formTable());

            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Comment"),
                    HtmlUtils.input(
                        ARG_CONTROLPOINTS_COMMENT, "", HtmlUtils.SIZE_60)));


            StringBuffer entryTable = new StringBuffer("<table>");
            entryTable.append("<tr><td align=center></td>");
            entryTable.append("<td colspan=3 align=center>");
            entryTable.append(HtmlUtils.checkbox(ARG_COORD_XYZ, "true",
                    true));
            entryTable.append(HtmlUtils.space(1));
            entryTable.append(msg("Include X/Y/Z"));
            entryTable.append("</td>");
            entryTable.append("<td colspan=3 align=center>");
            entryTable.append(HtmlUtils.checkbox(ARG_COORD_LONLATALT, "true",
                    true));
            entryTable.append(HtmlUtils.space(1));
            entryTable.append(msg("Include Lat/Lon/Elev"));
            entryTable.append("</td></tr>");
            entryTable.append(
                "<tr><td align=center></td><td align=center><b>X</b></td><td align=center><b>Y</b></td><td align=center><b>Z</b></td><td align=center><b>Longitude</b></td><td align=center><b>Latitude</b></td><td align=center><b>Elevation</b></td></tr>");

            for (Entry entry : entries) {
                if ( !isSolution(entry)) {
                    continue;
                }
                entryTable.append("<tr><td>");
                entryTable.append(HtmlUtils.checkbox(ARG_GPS_FILE,
                        entry.getId(), true));
                entryTable.append(" ");
                entryTable.append(entry.getName());
                entryTable.append("</td><td align=right>");
                entryTable.append(entry.getStringValue(request,SolutionTypeHandler.IDX_X,
                        "NA"));
                entryTable.append("</td><td align=right>");
                entryTable.append(entry.getStringValue(request,SolutionTypeHandler.IDX_Y,
                        "NA"));
                entryTable.append("</td><td align=right>");
                entryTable.append(entry.getStringValue(request,SolutionTypeHandler.IDX_Z,
                        "NA"));

                entryTable.append("</td><td align=right>");
                entryTable.append("" + entry.getLongitude(request));
                entryTable.append("</td>");
                entryTable.append("</td><td align=right>");
                entryTable.append("" + entry.getLatitude(request));
                entryTable.append("</td>");
                entryTable.append("</td><td align=right>");
                entryTable.append("" + entry.getAltitude());
                entryTable.append("</td></tr>");

            }
            entryTable.append("</table>");
            sb.append(HtmlUtils.formEntryTop(msgLabel("Entries"),
                                             entryTable.toString()));
            addPublishWidget(
                request, mainEntry, sb,
                msg(
                "Optionally select a folder to publish the control point file to"), true);
            sb.append(HtmlUtils.formTableClose());

            sb.append(HtmlUtils.submit("Make Control Point File"));
            sb.append(HtmlUtils.formClose());

            return new Result("", sb);
        }

        List<String> entryIds = request.get(ARG_GPS_FILE,
                                            new ArrayList<String>());

        StringBuffer buff = new StringBuffer();
        if (request.defined(ARG_CONTROLPOINTS_COMMENT)) {
            buff.append("#");
            buff.append(request.getString(ARG_CONTROLPOINTS_COMMENT, ""));
            buff.append("\n");
        }
        boolean anyOK = false;
        sb.append(msgHeader("Results"));
        sb.append("<ul>");
        List<Entry> solutionEntries = new ArrayList<Entry>();
        double      maxLat          = -90;
        double      minLat          = 90;
        double      maxLon          = -180;
        double      minLon          = 180;
        for (String entryId : entryIds) {
            Entry solutionEntry = getEntryManager().getEntry(request,
                                      entryId);
            if (solutionEntry == null) {
                throw new IllegalArgumentException("No entry:" + entryId);
            }

            if ( !isSolution(solutionEntry)) {
                sb.append("<li>");
                sb.append("Skipping:" + solutionEntry.getName());
                sb.append(HtmlUtils.p());

                continue;
            }

            maxLat = Math.max(maxLat, solutionEntry.getLatitude(request));
            minLat = Math.min(minLat, solutionEntry.getLatitude(request));
            maxLon = Math.max(maxLon, solutionEntry.getLongitude(request));
            minLon = Math.min(minLon, solutionEntry.getLongitude(request));

            solutionEntries.add(solutionEntry);
            anyOK = true;
            String siteCode =
                solutionEntry.getStringValue(request,SolutionTypeHandler.IDX_SITE_CODE, "");
            if (siteCode.length() == 0) {
                siteCode = solutionEntry.getName();
            }
            buff.append(siteCode);
            if (request.get(ARG_COORD_XYZ, false)) {
                buff.append(",");
                buff.append(solutionEntry.getStringValue(request,SolutionTypeHandler.IDX_X,
                        "NA"));
                buff.append(",");
                buff.append(solutionEntry.getStringValue(request,SolutionTypeHandler.IDX_Y,
                        "NA"));
                buff.append(",");
                buff.append(solutionEntry.getStringValue(request,SolutionTypeHandler.IDX_Z,
                        "NA"));
            }

            if (request.get(ARG_COORD_LONLATALT, false)) {
                buff.append(",");
                buff.append(solutionEntry.getLongitude(request));
                buff.append(",");
                buff.append(solutionEntry.getLatitude(request));
                buff.append(",");
                buff.append(solutionEntry.getAltitude());
            }
            buff.append("\n");
            //            buff.append(solutionEntry.getStringValue(SolutionTypeHandler.IDX_UTM_X,"NA"));
            //            buff.append(solutionEntry.getStringValue(SolutionTypeHandler.IDX_UTM_Y,"NA"));
            //            buff.append(solutionEntry.getAltitude());
        }

        sb.append("</ul>");
        if ( !anyOK) {
            return new Result("", sb);
        }


        if ( !doingPublish(request)) {
            request.setReturnFilename("controlpoints.csv");

            return new Result("", buff, "text/csv");
        }

        if (doingPublish(request)) {
            Entry parent = getEntryManager().findGroup(request,
                               request.getString(ARG_PUBLISH_ENTRY
                                   + "_hidden", ""));
            if (parent == null) {
                throw new IllegalArgumentException("Could not find folder");
            }
            if ( !getAccessManager().canDoNew(request, parent)) {
                throw new AccessException("No access", request);
            }
            String fileName = request.getString(ARG_PUBLISH_NAME, "").trim();
            if (fileName.length() == 0) {
                fileName = "controlpoints.csv";
            }
            //Write the text out
            File f = getStorageManager().getTmpFile(request, fileName);
            OutputStream out =
                getStorageManager().getUncheckedFileOutputStream(f);
            out.write(buff.toString().getBytes());
            out.flush();
            out.close();
            f = getStorageManager().copyToStorage(request, f, f.getName());

            TypeHandler typeHandler = getRepository().getTypeHandler(
                                          GpsTypeHandler.TYPE_CONTROLPOINTS);

            final double[] pts = { maxLat, minLon, minLat, maxLon };

            Entry newEntry = getEntryManager().addFileEntry(request, f,
							    parent, null, fileName, "",request.getUser(),
                                 typeHandler, new EntryInitializer() {
                public void initEntry(Entry entry) {
                    entry.setNorth(pts[0]);
                    entry.setWest(pts[1]);
                    entry.setSouth(pts[2]);
                    entry.setEast(pts[3]);
                }
            });
            sb.append("Control points file created:");
            sb.append(
                HtmlUtils.href(
                    HtmlUtils.url(
                        getRepository().URL_ENTRY_SHOW.toString(),
                        new String[] { ARG_ENTRYID,
                                       newEntry.getId() }), newEntry
                                           .getName()));

            getAuthManager().addAuthToken(request);
            for (Entry solutionEntry : solutionEntries) {
                getAssociationManager().addAssociation(request, newEntry,
                        solutionEntry, "", ASSOCIATION_TYPE_GENERATED_FROM);
            }
        }

        return new Result("", sb);
    }





    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    private boolean isTrimble(String file) {
        file = file.toLowerCase();

        return file.endsWith(".t00") || file.endsWith(".t01")
               || file.endsWith(".t02");
    }



    /**
     * _more_
     *
     * @param inputFile _more_
     * @param msgBuffer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getRawFile(String inputFile, StringBuffer msgBuffer)
            throws Exception {
        if ( !isTrimble(inputFile)) {
            return inputFile;
        }

        if ( !haveRunPkr()) {
            return null;
        }
        File datFile = getStorageManager().getTmpFile(
                           getRepository().getTmpRequest(),
                           IOUtil.getFileTail(
                               IOUtil.stripExtension(inputFile)) + ".dat");
	List<String> commands = (List<String>)Utils.makeListFromValues(runPkrPath, "-d", "-g",inputFile, datFile.toString());
	ProcessBuilder pb1 = getRepository().makeProcessBuilder(commands);
        Process process1 = pb1.start();
        String errorMsg =
            new String(IOUtil.readBytes(process1.getErrorStream()));
        String outMsg =
            new String(IOUtil.readBytes(process1.getInputStream()));
        process1.waitFor();
        inputFile = datFile.toString();
        msgBuffer.append("run pkr:" + errorMsg + " " + outMsg + " "
                         + datFile.exists() + " datFile:" + datFile);
        if ( !datFile.exists()) {
            msgBuffer.append("run pkr file does not exist " + errorMsg + " "
                             + outMsg);
        }

        return inputFile;
    }



    /**
     * _more_
     *
     * @param rinexFile _more_
     * @param flag _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String extractGpsMetadata(File rinexFile, String flag)
            throws Exception {
        String inputFile = getRawFile(rinexFile.toString(),
                                      new StringBuffer());
        if (inputFile == null) {
            //If its a trimble file and we don't have runpkr installed
            return "none";
        }

	List<String> commands = (List<String>)Utils.makeListFromValues(teqcPath, flag,inputFile);
	ProcessBuilder pb      = getRepository().makeProcessBuilder(commands);
        Process        process = pb.start();
        String errorMsg =
            new String(IOUtil.readBytes(process.getErrorStream()));
        String outMsg =
            new String(IOUtil.readBytes(process.getInputStream()));
        int result = process.waitFor();

        return outMsg;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param gpsTypeHandler _more_
     *
     * @throws Exception _more_
     */
    public void initializeGpsEntry(Entry entry, GpsTypeHandler gpsTypeHandler)
            throws Exception {
        //Check if we have teqc installed
        if ( !haveTeqc()) {
            return;
        }
        File gpsFile = entry.getFile();
        if ( !gpsFile.exists()) {
            return;
        }
        String   gpsMetadata = extractGpsMetadata(gpsFile, TEQC_FLAG_META);
        Object[] values      = gpsTypeHandler.getEntryValues(entry);
        //   2011-05-04 18:23:00.000
        //format,site_code,antenna_type,antenna_height

        //Initialize the values
        values[IDX_FORMAT]         = "";
        values[IDX_SITE_CODE]      = "";
        values[IDX_ANTENNA_TYPE]   = "";
        values[IDX_ANTENNA_HEIGHT] = Double.valueOf(0);

        for (String line : StringUtil.split(gpsMetadata, "\n", true, true)) {
            List<String> toks = StringUtil.splitUpTo(line, ":", 2);
            if (toks.size() < 2) {
                continue;
            }
            String key   = toks.get(0);
            String value = toks.get(1);
            if (value.trim().equals(TEQC_UNKNOWN)) {
                value = "";
            }
            //            System.err.println("KEY:" + key+":");
            if (key.equals("file format")) {
                values[IDX_FORMAT] = value;
            } else if (key.startsWith("start date")) {
                Date dttm = parseDate(value);
                if (dttm != null) {
                    entry.setStartDate(dttm.getTime());
                }
            } else if (key.startsWith("final date")) {
                Date dttm = parseDate(value);
                if (dttm != null) {
                    entry.setEndDate(dttm.getTime());
                }
            } else if (key.startsWith("4-char")) {
                values[IDX_SITE_CODE] = value;
            } else if (key.equals("antenna type")) {
                values[IDX_ANTENNA_TYPE] = value;
            } else if (key.startsWith("antenna height")) {
                values[IDX_ANTENNA_HEIGHT] = Double.parseDouble(value);
            } else if (key.startsWith("antenna latitude")) {
                double v = Double.parseDouble(value);
                if (v != 90) {
                    entry.setLatitude(v);
                }
            } else if (key.startsWith("antenna longitude")) {
                double v = Double.parseDouble(value);
                if (v != 0) {
                    entry.setLongitude(v);
                }
            } else if (key.startsWith("antenna elevation")) {
                double v = Double.parseDouble(value);
                //Check for bad values
                if (v > -100000) {
                    entry.setAltitude(v);
                }
            } else if (key.equals("")) {}
            else {
                //                System.err.println("key?:" + key + "=" + value);
            }
        }
    }

    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    private Date parseDate(String date) {
        //        2011-05-04 18:23:00.000
        try {
            SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            sdf.setTimeZone(getRepository().TIMEZONE_UTC);
            Date dttm = sdf.parse(date);

            return dttm;
        } catch (Exception exc) {
            System.err.println("Error:" + exc);

            return null;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputOpus(final Request request, final Entry mainEntry,
                              final List<Entry> entries)
            throws Exception {

        if (true) {
            throw new RuntimeException("Not Supported");
        }

        StringBuffer sb = new StringBuffer();
        if ( !request.get(ARG_OPUS_PROCESS, false)) {
            return outputOpusForm(request, mainEntry, entries, sb);
        }

        if ( !request.defined(ARG_OPUS_EMAIL)) {
            sb.append(
                getPageHandler().showDialogWarning("No email specified"));

            return outputOpusForm(request, mainEntry, entries, sb);
        }


        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                submitToOpus(request, mainEntry, entries, actionId);
            }
        };

        return getActionManager().doAction(request, action,
                                           "Uploading to OPUS", "");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    private void submitToOpus(Request request, Entry mainEntry,
                              List<Entry> entries, Object actionId)
            throws Exception {

        /**
         * ** Later when we have a post replacement
         *
         *
         * StringBuffer sb = new StringBuffer();
         * sb.append(msgHeader("Results"));
         * sb.append("<ul>");
         * StringBuffer extra = new StringBuffer();
         * int          cnt   = 0;
         * while (true) {
         *   String argSuffix = "_" + cnt;
         *   cnt++;
         *   if ( !request.exists(ARG_OPUS_ANTENNA + argSuffix)) {
         *       break;
         *   }
         *   String entryId = request.getString(ARG_RINEX_FILE + argSuffix,
         *                        "");
         *   if (entryId.equals("")) {
         *       continue;
         *   }
         *   Entry entry = getEntryManager().getEntry(request, entryId);
         *   if (entry == null) {
         *       throw new IllegalArgumentException("No entry:" + entryId);
         *   }
         *   sb.append("<li>");
         *   sb.append(entry.getName());
         *   if ( !isRinex(entry)) {
         *       sb.append(" ... skipping - not rinex");
         *
         *       continue;
         *   }
         *
         *   File f = entry.getFile();
         *   if ( !f.exists()) {
         *       sb.append(" ... skipping - file does not exist");
         *   }
         *
         *   List<HttpFormField> postEntries = new ArrayList<HttpFormField>();
         *   postEntries.add(HttpFormField.hidden(ARG_OPUS_EMAIL,
         *           request.getString(ARG_OPUS_EMAIL, "").trim()));
         *   String antenna = request.getString(ARG_OPUS_ANTENNA, "");
         *   if (antenna.equals(Antenna.NONE) || antenna.equals("")) {
         *       antenna = request.getString(ARG_OPUS_ANTENNA + argSuffix,
         *                                   Antenna.NONE);
         *   }
         *   postEntries.add(HttpFormField.hidden(ARG_OPUS_ANTENNA, antenna));
         *   String height = null;
         *   if (request.defined(ARG_OPUS_HEIGHT)) {
         *       height = request.getString(ARG_OPUS_HEIGHT, "");
         *   }
         *   if (height == null) {
         *       height = request.getString(ARG_OPUS_HEIGHT + argSuffix,
         *                                  "0.0");
         *   }
         *   postEntries.add(HttpFormField.hidden(ARG_OPUS_HEIGHT, height));
         *
         *   //            for data > 15 min. < 2 hrs. for data > 2 hrs. < 48 hrs.
         *   if (request.get(ARG_OPUS_RAPID, false)) {
         *       System.err.println("RAPID STATIC");
         *       postEntries.add(HttpFormField.hidden("Rapid-Static",
         *               "Upload to Rapid-Static"));
         *   } else {
         *       postEntries.add(HttpFormField.hidden("Static", "Static"));
         *   }
         *   postEntries.add(HttpFormField.hidden("theHost1",
         *           "www.ngs.noaa.gov"));
         *   postEntries.add(HttpFormField.hidden("", ""));
         *   postEntries.add(HttpFormField.hidden("selectList1", ""));
         *   postEntries.add(HttpFormField.hidden("extend_code", "0"));
         *   postEntries.add(HttpFormField.hidden("xml_code", "0"));
         *   postEntries.add(HttpFormField.hidden("set_profile", "0"));
         *   postEntries.add(HttpFormField.hidden("delete_profile", "0"));
         *   postEntries.add(HttpFormField.hidden("share", "2"));
         *   postEntries.add(HttpFormField.hidden("submit_database", "2"));
         *   postEntries.add(HttpFormField.hidden("opusOption", "0"));
         *   postEntries.add(HttpFormField.hidden("frameValue", "2011"));
         *   //Use the entry id so when we get the opus back we can look up the original entry
         *   String filename = request.getUser().getId() + "_" + entry.getId();
         *   filename = entry.getId();
         *   postEntries.add(
         *       new HttpFormField(
         *           "uploadfile", filename,
         *           IOUtil.readBytes(
         *               getStorageManager().getFileInputStream(f))));
         *
         *
         *   String   url      = OPUS_URL;
         *   String[] result   = { "", "" };
         *   String   errorMsg = null;
         *   try {
         *       result   = HttpFormField.doPost(postEntries, url, false);
         *       errorMsg = result[0];
         *   } catch (Exception exc) {
         *       errorMsg = LogUtil.getInnerException(exc).getMessage();
         *   }
         *
         *   if (errorMsg != null) {
         *       int idx = errorMsg.indexOf("errorMessage=");
         *       if (idx >= 0) {
         *           errorMsg = errorMsg.substring(idx
         *                   + "errorMessage=".length());
         *       }
         *   }
         *   String html = result[1];
         *   if (errorMsg != null) {
         *       //This is a hack since the httpformentry gets a redirect and tries to post again to the redirect url
         *       if (errorMsg.indexOf("uploadResults.jsp") >= 0) {
         *           //                    System.err.println("ERROR:" + errorMsg);
         *           errorMsg = null;
         *           html     = "Upload successful";
         *       }
         *   }
         *
         *   if (errorMsg != null) {
         *       sb.append(" ... Error:");
         *       sb.append(
         *           "<pre style=\"  border: solid 1px #000; max-height: 150px;overflow-y: auto; \">");
         *       errorMsg = StringUtil.stripTags(errorMsg);
         *       sb.append(errorMsg);
         *       sb.append("</pre>");
         *       sb.append("</ul>");
         *       getActionManager().setActionMessage(actionId, sb.toString());
         *
         *       break;
         *   } else {
         *       if (html.indexOf("Upload successful") >= 0) {
         *           sb.append(" ... Uploaded to OPUS. Height:" + height);
         *           if (antenna.equals(Antenna.NONE)) {
         *               sb.append(" No antenna selected");
         *           } else {
         *               sb.append(" Antenna:" + antenna);
         *           }
         *       } else {
         *           sb.append(" ... Error:");
         *           sb.append(result[1]);
         *           //                    System.out.println(result[1]);
         *       }
         *   }
         *   getActionManager().setActionMessage(actionId,
         *           sb.toString() + "</ul>");
         *   if ( !getActionManager().getActionOk(actionId)) {
         *       break;
         *   }
         * }
         *
         * sb.append("</ul>");
         * sb.append("When you get the results in your email click ");
         * sb.append(HtmlUtils.href(getRepository().getUrlBase()
         *                        + "/gps/addopus", "here"));
         * sb.append(" to upload the OPUS solutions");
         * sb.append(HtmlUtils.p());
         * sb.append(
         *   HtmlUtils.href(
         *       request.entryUrl(getRepository().URL_ENTRY_SHOW, mainEntry),
         *       msg("Return to Folder")));
         * getActionManager().setContinueHtml(actionId, sb.toString());
         */

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputOpusForm(Request request, Entry mainEntry,
                                  List<Entry> entries, StringBuffer sb)
            throws Exception {

        sb.append(HtmlUtils.p());
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_GPS_OPUS.getId()));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, mainEntry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OPUS_PROCESS, "true"));

        sb.append(HtmlUtils.submit("Submit to OPUS"));

        sb.append(HtmlUtils.formTable());
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Email"),
                HtmlUtils.input(
                    ARG_OPUS_EMAIL,
                    request.getString(
                        ARG_OPUS_EMAIL, request.getUser().getEmail()))));

        sb.append(HtmlUtils.formTableClose());
        //request.getString(ARG_OPUS_ANTENNA, entry.getStringValue(request,IDX_ANTENNA_TYPE,"")
        String selectedAntenna = "";
        for (Entry entry : entries) {
            if ( !isRinex(entry)) {
                continue;
            }
            selectedAntenna = (String) entry.getStringValue(request,IDX_ANTENNA_TYPE, "");

            break;
        }

        /*        sb.append(HtmlUtils.formEntry(msgLabel("Rapid"),
                  HtmlUtils.checkbox(ARG_OPUS_RAPID,
                  "true", request.get(ARG_OPUS_RAPID,false)) +" " +"for data &gt; 15 min. &lt; 2 hrs."));
        */

        StringBuffer entriesSB =
            new StringBuffer("<table cellpadding=3 cellspacing=3>");
        entriesSB.append(
            "<tr><td><b>Entry</b></td><td><b>Duration</b></td><td><b>Antenna</td></b<td><b>Height (meters)</b></td></tr>");
        List<String> selectedIds = request.get(ARG_RINEX_FILE,
                                       new ArrayList<String>());
        int cnt = 0;
        for (Entry entry : entries) {
            if ( !isRinex(entry)) {
                continue;
            }
            int minutes = (int) ((entry.getEndDate() - entry.getStartDate())
                                 / 1000l / 60l);
            boolean selected = true;
            if ((selectedIds.size() > 0)
                    && !selectedIds.contains(entry.getId())) {
                selected = false;
            }
            StringBuffer comment = new StringBuffer("");
            if ((minutes > 0) && (minutes < 120)) {
                selected = false;
                comment.append(HtmlUtils.italics("&lt; 2 hours. "));
            }
            if (getEntryUtil()
                    .getEntriesWithType(getAssociationManager()
                        .getTailEntriesWithAssociationType(request, entry,
                            GpsOutputHandler
                                .ASSOCIATION_TYPE_GENERATED_FROM), OpusTypeHandler
                                    .TYPE_OPUS).size() > 0) {
                if ( !request.isAnonymous()) {
                    selected = false;
                    comment.append(
                        HtmlUtils.italics("Already has an OPUS entry"));
                }
            }

            String argSuffix = "_" + cnt;
            cnt++;

            entriesSB.append("<tr><td>");
            entriesSB.append(HtmlUtils.checkbox(ARG_RINEX_FILE + argSuffix,
                    entry.getId(), selected));
            entriesSB.append(" ");
            entriesSB.append(entry.getName());
            entriesSB.append(" ");
            entriesSB.append(comment);
            entriesSB.append("</td><td align=right>");
            if (minutes != 0) {
                int hours = minutes / 60;
                minutes = minutes % 60;
                if (hours > 0) {
                    entriesSB.append(hours + ":");
                }
                if (minutes < 10) {
                    entriesSB.append("0" + minutes);
                } else {
                    entriesSB.append(minutes);
                }
            } else {
                entriesSB.append("NA");
            }
            entriesSB.append("</td>");
            entriesSB.append("<td align=right>");
            selectedAntenna = (String) entry.getStringValue(request,IDX_ANTENNA_TYPE, "");
            entriesSB.append(HtmlUtils.select(ARG_OPUS_ANTENNA + argSuffix,
                    Antenna.getAntennas(), selectedAntenna));
            entriesSB.append("</td>");
            entriesSB.append("<td align=right>");
            entriesSB.append(
                HtmlUtils.input(
                    ARG_OPUS_HEIGHT + argSuffix,
                    request.getString(
                        ARG_OPUS_HEIGHT + argSuffix,
                        entry.getStringValue(request,
                            IDX_ANTENNA_HEIGHT, "")), HtmlUtils.SIZE_5));
            entriesSB.append("</td>");

            entriesSB.append("</tr>");
        }
        entriesSB.append("</table>");
        sb.append(msgHeader("Select RINEX Files"));
        sb.append(entriesSB.toString());


        sb.append(msgHeader("Overrides"));
        sb.append(HtmlUtils.formTable());
        sb.append(
            HtmlUtils.formEntry(
                "", "If defined use these values for antenna or height"));
        sb.append(HtmlUtils.formEntry(msgLabel("Antenna"),
                                      HtmlUtils.select(ARG_OPUS_ANTENNA,
                                          Antenna.getAntennas(), "")));
        sb.append(HtmlUtils.formEntry(msgLabel("Antenna Height"),
                                      HtmlUtils.input(ARG_OPUS_HEIGHT,
                                          request.getString(ARG_OPUS_HEIGHT,
                                              ""), HtmlUtils.SIZE_5)));

        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit("Submit to OPUS"));
        sb.append(HtmlUtils.formClose());

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processAddOpus(Request request) throws Exception {

        StringBuffer sb = new StringBuffer();
        if (request.isAnonymous()) {
            sb.append(
                getPageHandler().showDialogError(
                    "You need to be logged in to add OPUS"));

            return new Result(OPUS_TITLE, sb);
        }
        if (request.exists(ARG_OPUS)) {
            StringBuffer msgBuff = new StringBuffer();
            Entry newEntry = processAddOpus(request,
                                            request.getString(ARG_OPUS, ""),
                                            msgBuff);
            if (newEntry == null) {
                sb.append(
                    getPageHandler().showDialogError(msgBuff.toString()));

                return processOpusForm(request, sb);
            }
            sb.append(HtmlUtils.p());
            sb.append("OPUS entry created: ");
            sb.append(
                HtmlUtils.href(
                    HtmlUtils.url(
                        getRepository().URL_ENTRY_SHOW.toString(),
                        new String[] { ARG_ENTRYID,
                                       newEntry.getId() }), newEntry
                                           .getName()));

            return new Result(OPUS_TITLE, sb);
        }

        return processOpusForm(request, sb);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param opus _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry processAddOpus(Request request, String opus, StringBuffer sb)
            throws Exception {

        //FILE: admin_2cc34996-d976-46d3-88b2-559fe3460f63 0

        String suffix = StringUtil.findPattern(opus,
                            "FILE:\\s*([^\\s\\.]+)(\\s|\\.rinex)");
        if (suffix == null) {
            System.err.println("OPUS:" + opus);
            sb.append("Could not find FILE name in the given OPUS");

            return null;
        }

        String user         = null;
        String rinexEntryId = null;
        //We encode the user id and rinex entry id in the posted filename
        if (suffix.indexOf("_") >= 0) {
            user = StringUtil.findPattern(suffix, "([^_]+)_");
            if (user == null) {
                sb.append("Could not find user id in: " + suffix);

                return null;
            }
            rinexEntryId = StringUtil.findPattern(suffix, "[^_]+_(.*)$");
            if (rinexEntryId == null) {
                sb.append("Could not find rinex entry id in: " + suffix);

                return null;
            }
        } else {
            rinexEntryId = suffix;
        }

        if (rinexEntryId == null) {
            rinexEntryId = suffix;
        }

        if (request == null) {
            if (user == null) {
                sb.append("Could not find user id in OPUS results");

                return null;
            }
            request = getRepository().getTmpRequest(user);
            //            System.err.println("tmp request:" + request.getUser() +" admin="+ request.getUser().getAdmin());
        }

        //        System.err.println("request:" + request.getUser() +" admin="+ request.getUser().getAdmin());


        final Entry rinexEntry = getEntryManager().getEntry(request,
                                     rinexEntryId);
        if (rinexEntry == null) {
            sb.append("Could not find original RINEX entry: " + rinexEntryId);

            return null;
        }
        Entry parentEntry = rinexEntry.getParentEntry();

        //Look for the OPUS sibling folder
        for (Entry child :
                getEntryManager().getChildrenGroups(request,
                    parentEntry.getParentEntry())) {
            if (child.getName().toLowerCase().trim().equals("solutions")) {
                parentEntry = child;

                break;
            }
            if (child.getName().toLowerCase().trim().equals("opus")) {
                parentEntry = child;

                break;
            }
        }

        if ( !getAccessManager().canDoNew(request, parentEntry)) {
            sb.append("You do not have permission to add to:"
                      + parentEntry.getName());

            return null;
        }

        String opusFileName = IOUtil.stripExtension(rinexEntry.getName())
                              + ".opus";
        //Write the text out
        File         f = getStorageManager().getTmpFile(request,
                             opusFileName);
        OutputStream out = getStorageManager().getFileOutputStream(f);
        out.write(opus.getBytes());
        out.flush();
        out.close();
        f = getStorageManager().copyToStorage(request, f, f.getName());

        TypeHandler typeHandler =
            getRepository().getTypeHandler(OpusTypeHandler.TYPE_OPUS);

        final Object     siteCode    = rinexEntry.getStringValue(request,IDX_SITE_CODE, "");
        EntryInitializer initializer = new EntryInitializer() {
            public void initEntry(Entry entry) {
                entry.getTypeHandler().getEntryValues(
                    entry)[OpusTypeHandler.IDX_SITE_CODE] = siteCode;
                entry.setStartDate(rinexEntry.getStartDate());
                entry.setEndDate(rinexEntry.getEndDate());
            }
        };
        Entry newEntry = getEntryManager().addFileEntry(request, f,
							parentEntry, null, opusFileName, "", request.getUser(),
                             typeHandler, initializer);


        boolean canEditRinex = getAccessManager().canDoEdit(request, rinexEntry);

        //If we figured out location from the opus file then set the rinex entry location


        if (newEntry.hasLocationDefined(request)) {
            if (canEditRinex) {
                rinexEntry.setLocation(newEntry.getLatitude(request),
                                       newEntry.getLongitude(request),
                                       newEntry.getAltitude());
                getEntryManager().updateEntry(request, rinexEntry);
            }

            for (Entry rawEntry :
                    getEntryUtil()
                        .getEntriesWithType(getAssociationManager()
                            .getTailEntriesWithAssociationType(request,
                                rinexEntry,
                                GpsOutputHandler
                                    .ASSOCIATION_TYPE_GENERATED_FROM), GpsTypeHandler
                                        .TYPE_RAW)) {
                if (getAccessManager().canDoEdit(request, rawEntry)) {
                    rawEntry.setLocation(newEntry.getLatitude(request),
                                         newEntry.getLongitude(request),
                                         newEntry.getAltitude());
                    getEntryManager().updateEntry(request, rawEntry);
                }
            }
        }
        getAuthManager().addAuthToken(request);
        getAssociationManager().addAssociation(request, newEntry, rinexEntry,
                "generated rinex",
                GpsOutputHandler.ASSOCIATION_TYPE_GENERATED_FROM);

        return newEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processOpusForm(Request request, StringBuffer sb)
            throws Exception {
        String base    = getRepository().getUrlBase();
        String formUrl = base + URL_ADDOPUS;
        sb.append(HtmlUtils.p());
        sb.append("Enter the OPUS solution text below");
        sb.append(HtmlUtils.formPost(formUrl));
        sb.append(HtmlUtils.submit("Add OPUS Solution"));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.textArea(ARG_OPUS, "", 20, 70));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.submit("Add OPUS Solution"));
        sb.append(HtmlUtils.formClose());

        return new Result(OPUS_TITLE, sb);
    }




}
