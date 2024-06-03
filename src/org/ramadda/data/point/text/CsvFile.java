/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;

import org.ramadda.repository.Entry;
import org.ramadda.data.point.*;
import org.ramadda.data.record.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * CSV file supports any form of column delimited files - comma, tab, space, etc
 *
 */
@SuppressWarnings("unchecked")
public class CsvFile extends TextFile {


    public static final String DELIMITER_SPACE = "space";
    public static final String DELIMITER_SPACES = "spaces";    


    boolean debug             = false;
    boolean debugCsvFile      = false;    

    /** column delimiter */
    private String delimiter = null;


    /**  */
    private boolean hasCsvCommands = false;

    /**  */
    private boolean hasAddHeader = false;

    static int fcnt = 0;

    /**
     * ctor
     */
    public CsvFile() {
    }


    /**
     * ctor
     *
     * @throws IOException on badness
     */
    public CsvFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * ctor
     *
     * @param properties _more_
     *
     * @throws IOException on badness
     */
    public CsvFile(IO.Path path, Hashtable properties) throws IOException {
        super(path, properties);
    }


    /**
     * _more_
     *
     * @param context _more_
     * @param properties _more_
     */
    public CsvFile(IO.Path path, RecordFileContext context,
                   Hashtable properties) {
        super(path, context, properties);
    }




    private String convertCsvCommands(String cmd) {
	if(cmd==null) return null;
	if(cmd.indexOf("${latitude}")>=0) {
	    cmd = cmd.replace("${latitude}",(String)getProperty("latitude","NaN"));
	    cmd = cmd.replace("${longitude}",(String)getProperty("longitude","NaN"));		
	}
	return cmd;

    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getCsvCommands() throws Exception {
        List<String>  args      = new ArrayList<String>();
        StringBuilder commands  = new StringBuilder();
        int           appendCnt = 0;
        String csvCommands = getProperty("csvcommands",
                                         getProperty("point.csvcommands",
                                             (String) null));
	csvCommands = convertCsvCommands(csvCommands);

        if (Utils.stringDefined(csvCommands)) {
	    commands.append(csvCommands);
            appendCnt++;
        }
        int commandCnt = 1;
        while (true) {
            String c = getProperty("csvcommands" + (commandCnt++),
                                   (String) null);
            if ( !Utils.stringDefined(c)) {
                break;
            }
	    c = convertCsvCommands(c);
            if (appendCnt > 0) {
                if (commands.length() > 0) {
                    commands.append(",");
                }
            }
            commands.append(c);
            appendCnt++;
        }
        if (appendCnt == 0) {
            return args;
        }
        csvCommands = commands.toString().trim().replaceAll("\\\\,",
                "_comma_");
        for (String arg : Utils.split(csvCommands, ",")) {
            args.add(arg.replaceAll("_comma_", ",").replaceAll("_csvcommandspace_"," "));
        }
        return args;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldCreateCsvFile() {
        return false;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param fos _more_
     * @param buffered _more_
     * @param commands _more_
     *
     * @throws Exception _more_
     */
    protected void doCreateCsvFile(File file, OutputStream fos,
                                   boolean buffered, List<String> commands)
            throws Exception {
        if ( !commands.get(commands.size() - 1).equals("-print")) {
            commands.add("-print");
        }
	if(debug)
	    System.err.println("making Seesv:" + commands);
	commands = preprocessCsvCommands(commands);
        Seesv seesv = new Seesv(commands,
                                      new BufferedOutputStream(fos), null);

        RecordFileContext ctx = getRecordFileContext();
        if (ctx != null) {
            seesv.setPropertyProvider(ctx.getPropertyProvider());
        }
        //else   System.err.println("No RecordFileContext set");
        runSeesv(seesv, buffered);
        fos.flush();
        fos.close();
    }


    public InputStream applySeesv(Entry entry, String[] seesvArgs) throws Exception {
	File file = getCacheFile();
	synchronized(this) {
	    if (file.exists())  return new FileInputStream(file);
	}
	File tmp = entry.getTypeHandler().getStorageManager().getTmpFile("tmp.csv");
	InputStream is = makeInputStream(true);
	OutputStream os = new FileOutputStream(tmp);
	new Seesv(seesvArgs, os,null).setInputStream(is).run(null);
	IO.close(is,os);
	synchronized(this) {
	    tmp.renameTo(file);
	}
	return new FileInputStream(file);
    }



    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
        List<String> commands     = getCsvCommands();
        boolean      shouldCreate = shouldCreateCsvFile();
        if ( !shouldCreate && (commands.size() == 0)) {

            if ( !getProperty("isRAMADDAPointData", "false").equals("true")
                    && !isHeaderStandard()) {
                setFirstLineFields(true);
                if (debug) {
                    System.err.println(
                        "CsvFile.doMakeInputStream: no commands setFirstLineFields: true");
                }
            } else {
                if (debug) {
                    System.err.println(
                        "CsvFile.doMakeInputStream: no commands setFirstLineFields: false");
                }
            }

            return super.doMakeInputStream(buffered);
        }


        hasAddHeader = commands.contains("-addheader");
        if (debug) {
            System.err.println(
                "CsvFile.doMakeInputStream commands: hasAddHeader:"
                + hasAddHeader);
	    StringBuilder sb = new StringBuilder();
	    for(String s: commands) {
		s= s.trim();
		if(s.indexOf(" ")>=0) s="\"" + s +"\"";
		sb.append(s);
		sb.append(" ");
	    }
	    System.err.println("csv commands:" + sb);
        }
        hasCsvCommands = true;
        File file = checkCachedFile();
        if ((file == null) || !file.exists()) {
            try {
                if (debug || debugCsvFile) {
                    System.err.println("Creating CSV cached file: " + file
                                       + "\ncommands:" + commands);
                }
                ByteArrayOutputStream bos = null;
                OutputStream          fos;
                if (file != null) {
                    fos = new FileOutputStream(file);
                    filesBeingWritten.put(file, "");
                } else {
                    fos = bos = new ByteArrayOutputStream();
                }
                doCreateCsvFile(file, fos, buffered, commands);
                if (file == null) {
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            } finally {
                if (file != null) {
                    filesBeingWritten.remove(file);
                }
            }
        } else {
            if (debug) {
                System.err.println("CSV file was cached: " + file);
            }
        }

        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * _more_
     *
     * @param csvUtil _more_
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream doMakeInputStream(Seesv csvUtil, boolean buffered)
            throws Exception {
        return super.doMakeInputStream(buffered);
    }

    /**
     * _more_
     *
     * @param csvUtil _more_
     * @param buffered _more_
     *
     * @throws Exception _more_
     */
    public void runSeesv(Seesv csvUtil, boolean buffered)
            throws Exception {
	InputStream inputStream =doMakeInputStream(csvUtil, buffered);
	csvUtil.setInputStream(inputStream);
	csvUtil.run(null);
	setHeaderLines(csvUtil.getHeaderLines());
    }

    public List<String>  preprocessCsvCommands(List<String>  commands) throws Exception {
	return commands;
    }


    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     */
    public boolean canLoad(String filename) {
        String f = filename.toLowerCase();
        //A hack to not include lidar coordinate txt files
        if ((f.indexOf("coords") >= 0) || (f.indexOf("coordinates") >= 0)) {
            return false;
        }
        if (f.indexOf("target") >= 0) {
            return false;
        }

        return (f.endsWith(".csv") || f.endsWith(".txt")
                || f.endsWith(".xyz") || f.endsWith(".tsv"));
    }

    /**
     * is this file capable of certain actions - gridding, decimation, etc
     *
     * @param action action type
     *
     * @return is capable
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_GRID)) {
            return true;
        }
        if (action.equals(ACTION_DECIMATE)) {
            return true;
        }

        return super.isCapable(action);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getDelimiter() {
        if (delimiter == null) {
            delimiter = getProperty(PROP_DELIMITER, ",");
            if (delimiter.length() == 0) {
                delimiter = " ";
            } else if (delimiter.equals("\\t")) {
                delimiter = "\t";
            } else if (delimiter.equals("tab")) {
                delimiter = "\t";
            }
        }

        return delimiter;
    }


    /**
     * _more_
     */
    public void initAfterClone() {
        super.initAfterClone();
        delimiter = null;
    }

    /**
     *  @return _more_
     */
    public boolean getFirstLineFields() {
        String fieldString = getProperty(PROP_FIELDS, null);
        if (fieldString != null) {
            if (debug) {
                System.err.println(
                    "CsvFile.getFirstLineFields: has fields property:" + fieldString);
            }

            return false;
        }
        if (hasAddHeader) {
            if (debug) {
                System.err.println(
                    "CsvFile.getFirstLineFields: hasAddHeader=true");
            }

            return false;
        }
        if (debug) {
            System.err.println(
                "CsvFile.getFirstLineFields: has csv commands="
                + hasCsvCommands + " super:" + super.getFirstLineFields());
        }

        return hasCsvCommands || super.getFirstLineFields();
    }

    public String getFieldsProperty() {
        return  getProperty(PROP_FIELDS, null);
    }


    /**
     * _more_
     *
     * @param failureOk _more_
     *
     * @return _more_
     */
    @Override
    public List<RecordField> doMakeFields(boolean failureOk) {
        String fieldString = getFieldsProperty();
        if (debug) {
            System.err.println("CsvFile.doMakeFields fieldString:"
                               + fieldString);
        }
        //      System.err.println("CsvFile.doMakeFields props:" + getProperties());
        if (fieldString == null) {
            doQuickVisit();
            fieldString = getProperty(PROP_FIELDS, null);
        } else {
            //Read the header because there are properties
            if (getHeaderLines().size() == 0) {
                if (getProperty(PROP_HEADER_STANDARD, false)) {
                    doQuickVisit();
                }
            }
        }

        commentLineStart = getProperty("commentLineStart", null);
        if (fieldString == null) {
            setIsHeaderStandard(true);
            doQuickVisit();
            fieldString = getProperty(PROP_FIELDS, null);
        }

        if (fieldString == null) {
            if ( !getProperty("fieldsCanBeNull", false)) {
                if (failureOk) {
                    return new ArrayList<RecordField>();
                }
                System.err.println("Error in CsvFile:" +" no " + PROP_FIELDS
                                   + " properties found for file: "
                                   + getPath());

                throw new IllegalArgumentException("No fields defined for file");
            }
            fieldString = "";
        }

        return doMakeFields(fieldString);
    }

    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @return _more_
     */
    @Override
    public BaseRecord doMakeRecord(VisitInfo visitInfo) {
        TextRecord record = new TextRecord(this, getFields());
	if(getProperty("cleanInput",false)) {
	    record.setCleanInput(true);
	}
        if (getBaseDate() != null) {
            record.setBaseDate(getBaseDate());
        }
        record.setFirstDataLine(firstDataLine);
        record.setDelimiter(getDelimiter());
        record.setLineWrap(getProperty("lineWrap", false));
        record.setBePickyAboutTokens(getProperty("picky", true));
        record.setMatchUpColumns(getProperty("matchupColumns", false));

        return record;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception on badness
     */
    public static void main(String[] args) throws Exception {
        if (true) {
            PointFile.test(args, CsvFile.class);

            return;
        }


        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            String arg = args[argIdx]
            ;
            try {
                long                t1       = System.currentTimeMillis();
                final int[]         cnt      = { 0 };
                CsvFile             file     = new CsvFile(new IO.Path(arg));
                final RecordVisitor metadata = new RecordVisitor() {
                    public boolean visitRecord(RecordFile file,
                            VisitInfo visitInfo, BaseRecord record) {
                        cnt[0]++;
                        PointRecord pointRecord = (PointRecord) record;
                        if ((pointRecord.getLatitude() < -90)
                                || (pointRecord.getLatitude() > 90)) {
                            System.err.println("Bad lat:"
                                    + pointRecord.getLatitude());
                        }
                        if ((cnt[0] % 100000) == 0) {
                            System.err.println(cnt[0] + " lat:"
                                    + pointRecord.getLatitude() + " "
                                    + pointRecord.getLongitude() + " "
                                    + pointRecord.getAltitude());

                        }

                        return true;
                    }
                };
                file.visit(metadata);
                long t2 = System.currentTimeMillis();
                System.err.println("time:" + (t2 - t1) / 1000.0
                                   + " # record:" + cnt[0]);
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();
            }
        }
    }






}
