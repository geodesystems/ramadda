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

package org.ramadda.data.record;


import org.ramadda.data.record.filter.*;


import org.ramadda.util.Utils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;


/**
 * The core class. This represents a file that can either be read or written.
 * It holds a filename and will create a RecordIO when dealing with files.
 * Derived classes need to overwrite the doMakeRecord method.
 *
 *
 * @author Jeff McWhirter
 */
public abstract class RecordFile {


    /** _more_ */
    public static final String PROP_DATEFORMAT = "dateformat";

    /** _more_ */
    public static final String PROP_UTCOFFSET = "utcoffset";

    /** _more_ */
    public static final String PROP_PRECISION = "precision";

    /** _more_ */
    public static final String PROP_FORMAT = "format";



    /** force compile */
    private static final RecordVisitorGroup dummy1 = null;

    /** force compile */
    private static final RecordCountVisitor dummy2 = null;

    /** force compile */
    private static final GeoRecord dummy3 = null;


    /** debug */
    public static boolean debug = false;

    /** The file */
    private String filename;

    /** default skip factor */
    private int defaultSkip = -1;

    /** how many records in the file */
    private long numRecords = 0;

    /** general properties */
    private Hashtable properties;

    /** _more_ */
    private Hashtable requestProperties;

    /** _more_ */
    private Object[] fileMetadata;

    /** _more_ */
    private String descriptionFromFile;

    /** _more_ */
    private String nameFromFile;

    /** _more_ */
    private Hashtable fileProperties = new Hashtable();


    /** _more_ */
    private int[] ymdhmsIndices;

    /** _more_ */
    private int dateIndex = -1;

    /** _more_ */
    private int timeIndex = -1;

    /** _more_ */
    private StringBuffer dttm = new StringBuffer();

    /** _more_ */
    private SimpleDateFormat[] mySdfs;

    /** _more_ */
    private static SimpleDateFormat[][] SDFS = {
        { makeDateFormat("yyyy") },
        { makeDateFormat("yyyy-MM"), makeDateFormat("yyyy-MMM"),
          makeDateFormat("yyyy-MMMM"), }, { makeDateFormat("yyyy-MM-dd") },
        { makeDateFormat("yyyy-MM-dd-HH") },
        { makeDateFormat("yyyy-MM-dd-HH-mm") },
        { makeDateFormat("yyyy-MM-dd-HH-mm-ss") },
    };


    /** _more_          */
    private RecordFileContext context;

    /** _more_          */
    private File cacheFile;

    /**
     * ctor
     */
    public RecordFile() {}

    /**
     * _more_
     *
     * @param properties _more_
     */
    public RecordFile(Hashtable properties) {
        this.properties = properties;
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param properties _more_
     */
    public RecordFile(String filename, Hashtable properties) {
        this.filename   = filename;
        this.properties = properties;
    }


    /**
     * ctor
     *
     *
     * @param filename The file
     *
     */
    public RecordFile(String filename) {
        this.filename = filename;
    }


    /**
     * _more_
     *
     * @param filename _more_
     * @param context _more_
     * @param properties _more_
     */
    public RecordFile(String filename, RecordFileContext context,
                      Hashtable properties) {
        this.filename   = filename;
        this.properties = properties;
        this.context    = context;
    }

    /**
     * Set the CacheFile property.
     *
     * @param value The new value for CacheFile
     */
    public void setCacheFile(File value) {
        cacheFile = value;
    }

    /**
     * Get the CacheFile property.
     *
     * @return The CacheFile
     */
    public File getCacheFile() {
        return cacheFile;
    }


    /**
     * _more_
     *
     * @param field _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getContextProperty(RecordField field, String key,
                                     String dflt) {
        if (context == null) {
            return dflt;
        }
        String v = context.getFieldProperty(field.getName(), key);
        if (v == null) {
            return dflt;
        }

        return v;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object[] getFileMetadata() {
        return fileMetadata;
    }

    /**
     * _more_
     *
     * @param metadata _more_
     */
    public void setFileMetadata(Object[] metadata) {
        fileMetadata = metadata;
    }




    /**
     * _more_
     */
    public void initAfterClone() {
        fields = null;
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException On badness
     * @throws Exception _more_
     */
    public RecordFile cloneMe(String filename, Hashtable properties,
                              Hashtable requestProperties)
            throws CloneNotSupportedException, Exception {
        RecordFile that = cloneMe();
        that.initAfterClone();
        that.setFilename(filename);
        this.requestProperties = requestProperties;
        if (properties == null) {
            properties = getPropertiesForFile(filename,
                    that.getPropertiesFileName());
        }
        that.setProperties(properties);

        return that;
    }

    /** _more_ */
    private Hashtable<String, String> classProperties;

    /**
     * _more_
     *
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public void initClassProperties(Hashtable<String, String> props)
            throws Exception {}


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Hashtable getClassProperties() throws Exception {
        if (classProperties == null) {
            String propertiesFile = "/"
                                    + getClass().getName().replaceAll("\\.",
                                        "/") + ".properties";

            Hashtable<String, String> tmp = new Hashtable<String, String>();
            initClassProperties(tmp);
            tmp.putAll(
                Utils.getProperties(IOUtil.readContents(propertiesFile, "")));
            classProperties = tmp;
        }

        return classProperties;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPropertiesFileName() {
        return "record.properties";
    }



    /**
     * _more_
     *
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Hashtable getProperties(File[] files) throws Exception {
        Hashtable p = new Hashtable();
        for (File f : files) {
            if ( !f.exists()) {
                continue;
            }
            String contents = IOUtil.readContents(f);
            p.putAll(Utils.getProperties(contents));
        }

        return p;
    }





    /**
     * _more_
     *
     * @param file _more_
     * @param defaultCommonFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Hashtable getPropertiesForFile(String file,
            String defaultCommonFile)
            throws Exception {
        File   f      = new File(file);
        File   parent = f.getParentFile();
        String commonFile;
        if (parent == null) {
            commonFile = defaultCommonFile;
        } else {
            commonFile = parent + File.separator + defaultCommonFile;
        }
        File[] propertiesFiles = new File[] { new File(commonFile),
                new File(IOUtil.stripExtension(file) + ".properties"),
                new File(file + ".properties"), };

        return getProperties(propertiesFiles);
    }



    /**
     * clone me
     *
     * @return my clone
     *
     * @throws CloneNotSupportedException On badness
     */
    public RecordFile cloneMe() throws CloneNotSupportedException {
        return (RecordFile) super.clone();
    }


    /**
     * Can this recordfile load  the given file
     *
     * @param file the file
     *
     * @return default is false
     */
    public boolean canLoad(String file) {
        return false;
    }

    /**
     * Check to see if this file is capable of the given action
     *
     * @param action the action
     *
     * @return is capable
     */
    public boolean isCapable(String action) {
        String p = (String) getProperty(action);
        if (p != null) {
            return p.trim().equals("true");
        }

        return false;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param suffixes _more_
     * @param checkForNumberSuffix _more_
     *
     * @return _more_
     */
    public boolean canLoad(String file, String[] suffixes,
                           boolean checkForNumberSuffix) {
        for (String suffix : suffixes) {
            if (file.endsWith(suffix)) {
                return true;
            }
        }
        if ( !checkForNumberSuffix) {
            return false;
        }
        file = file.trim();
        while (file.matches(".*\\.\\d+\\z")) {
            file = IOUtil.stripExtension(file);
            for (String suffix : suffixes) {
                if (file.endsWith(suffix)) {
                    return true;
                }
            }
        }

        return false;
    }



    /**
     * Get a property
     *
     * @param key property key
     *
     * @return property value
     */
    public Object getProperty(Object key) {
        if (properties == null) {
            return null;
        }

        return properties.get(key);
    }


    /**
     * _more_
     *
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String prop, String dflt) {
        return getProperty(properties, prop, dflt);
    }


    /**
     * _more_
     *
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String prop, boolean dflt) {
        return getProperty(prop, "" + dflt).equals("true");
    }


    /**
     * _more_
     *
     * @param field _more_
     * @param properties _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(RecordField field, Hashtable properties,
                              String prop, String dflt) {
        String value = getProperty(properties, prop, (String) null);
        if (value == null) {
            String fieldProp = "field." + field.getName() + "." + prop;
            value = (String) getProperty(fieldProp, (String) null);
        }
        if (value == null) {
            value = (String) getProperty(prop, (String) null);
        }
        if (value == null) {
            value = getContextProperty(field, prop, dflt);
        }


        if (value == null) {
            value = dflt;
        }

        return value;
    }


    /**
     * _more_
     *
     * @param properties _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(Hashtable properties, String prop,
                              String dflt) {
        if (properties == null) {
            return dflt;
        }
        String value = (String) properties.get(prop);
        if (value != null) {
            return value;
        }

        return dflt;
    }



    /**
     * put a property
     *
     * @param key key
     * @param value value
     */
    public void putProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key, value);
    }


    /**
     * _more_
     *
     * @param buff _more_
     *
     * @throws Exception On badness
     */
    public void getInfo(Appendable buff) throws Exception {}

    /**
     * Make the input io object
     *
     * @return The RecordIO for reading the file
     *
     * @throws IOException On badness
     */
    public RecordIO doMakeInputIO() throws IOException {
        return doMakeInputIO(false);
    }


    /**
     * Make the input io object
     *
     * @param buffered If true then make a bufferedinputstream
     *
     * @return The RecordIO
     *
     * @throws IOException On badness
     */
    public RecordIO doMakeInputIO(boolean buffered) throws IOException {
        return new RecordIO(doMakeInputStream(buffered));
    }


    /**
     * Make the input stream
     *
     * @param buffered If true then make a BufferedInputStream wrapper around the InputStream
     *
     * @return The input stream
     *
     * @throws IOException On badness
     */
    public InputStream doMakeInputStream(boolean buffered)
            throws IOException {
        //        System.err.println("****  RecordFile reading:" + filename);
        String path = getNormalizedFilename();
        //A hack for snotel data
        if (path.startsWith("http://www.wcc.nrcs.usda.gov")) {
            path = path.replace("http://www.wcc.nrcs.usda.gov",
                                "https://wcc.sc.egov.usda.gov");
        }

        return Utils.doMakeInputStream(path, buffered);
    }



    /**
     * Make the output stream. Note - this always makes a BufferedOutputStream wrapper around the
     * FileOutputStream
     *
     * @return The output stream
     *
     * @throws IOException On badness
     */
    public DataOutputStream doMakeOutputStream() throws IOException {
        return new DataOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(getNormalizedFilename()), 10000));
    }

    /**
     * _more_
     *
     * @param properties _more_
     */
    public void setProperties(Hashtable properties) {
        this.properties = properties;
    }


    /**
     * Make a Record. This calls the doMakeRecord factory method to actually
     * make the record. It then sets the quickscan flag on the record if needed
     *
     * @param visitInfo Holds visit state
     *
     * @return The record
     */
    public Record makeRecord(VisitInfo visitInfo) {
        Record record = doMakeRecord(visitInfo);
        if (visitInfo.getQuickScan()) {
            //            System.err.println("quick scan");
            record.setQuickScan(true);
        } else {
            //            System.err.println("full scan");
        }

        return record;
    }



    /**
     * Factory method for creating a Record object.
     *
     *
     * @param visitInfo Visit state
     * @return The Record object
     */
    public abstract Record doMakeRecord(VisitInfo visitInfo);


    /** _more_ */
    private List<RecordField> fields;



    /**
     * Get the fields for the default record
     *
     * @return List of fields in the default record
     */
    public List<RecordField> getFields() {
        return getFields(false);
    }


    /**
     * _more_
     *
     * @param failureOk _more_
     *
     * @return _more_
     */
    public List<RecordField> getFields(boolean failureOk) {
        if (fields == null) {
            fields = doMakeFields(failureOk);
        }

        return fields;
    }


    /**
     * _more_
     *
     * @param f _more_
     */
    public void setFields(List<RecordField> f) {
        fields = f;
    }

    /**
     * _more_
     *
     *
     * @param failureOk _more_
     * @return _more_
     */
    public List<RecordField> doMakeFields(boolean failureOk) {
        Record            record = makeRecord(new VisitInfo());
        List<RecordField> fields = record.getFields();

        return new ArrayList<RecordField>(fields);
    }


    /**
     * Get the fields that are marked as searchable
     *
     * @return List of searchable fields
     */
    public List<RecordField> getSearchableFields() {
        List<RecordField> fields = getFields();
        List<RecordField> result = new ArrayList<RecordField>();
        for (RecordField attr : fields) {
            if (attr.getSearchable()) {
                result.add(attr);
            }
        }

        return result;
    }




    /**
     * Get the fields that are marked as being chartable. Note - we should generalize this as finding
     * fields with a certain flag set.
     *
     * @return Chartable fields
     */
    public List<RecordField> getChartableFields() {
        List<RecordField> fields = getFields();
        List<RecordField> result = new ArrayList<RecordField>();
        for (RecordField attr : fields) {
            if (attr.getChartable()) {
                result.add(attr);
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param currentRecord _more_
     *
     * @return _more_
     */
    public Record makeNextRecord(Record currentRecord) {
        return currentRecord;
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        return visitInfo;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public VisitInfo doMakeVisitInfo() {
        return new VisitInfo();
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public int getSkip(VisitInfo visitInfo) {

        if ((visitInfo != null) && (visitInfo.getSkip() >= 0)) {
            return visitInfo.getSkip();
        }
        if (defaultSkip >= 0) {
            return defaultSkip;
        }

        return 0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSkip() {
        return defaultSkip;
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Record.ReadStatus readNextRecord(VisitInfo visitInfo,
                                            Record record)
            throws Exception {
        visitInfo.addRecordIndex(1);
        Record.ReadStatus status =
            record.readNextRecord(visitInfo.getRecordIO());

        return status;
    }

    /**
     * _more_
     */
    public void doQuickVisit() {
        try {
            RecordIO  recordIO     = doMakeInputIO(true);
            VisitInfo tmpVisitInfo = new VisitInfo();
            tmpVisitInfo.setRecordIO(recordIO);
            prepareToVisit(tmpVisitInfo);
            recordIO.close();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     *
     * @param visitor _more_
     * @throws Exception On badness
     */
    public void visit(RecordVisitor visitor) throws Exception {
        visit(visitor, doMakeVisitInfo(), null);
    }


    /**
     * _more_
     *
     * @param visitor _more_
     * @param filter _more_
     *
     * @throws Exception On badness
     */
    public void visit(RecordVisitor visitor, RecordFilter filter)
            throws Exception {
        visit(visitor, doMakeVisitInfo(), filter);
    }



    /**
     * _more_
     *
     * @param visitor _more_
     * @param visitInfo _more_
     * @param filter _more_
     *
     * @throws Exception On badness
     */
    public void visit(RecordVisitor visitor, VisitInfo visitInfo,
                      RecordFilter filter)
            throws Exception {

        //        System.err.println("RecordFile: " + getClass().getName() + ".visit");
        if (visitInfo == null) {
            visitInfo = new VisitInfo();
        }
        int skip = getSkip(visitInfo);
        //        System.err.println("RecordFile.visit skip =" + skip);
        RecordIO recordIO = doMakeInputIO(skip == 0);
        visitInfo.setRecordIO(recordIO);
        visitInfo = prepareToVisit(visitInfo);
        if (visitInfo == null) {
            return;
        }
        //Start at -1 since we always increment below
        visitInfo.setRecordIndex(-1);
        boolean ok = true;
        try {
            Record record = makeRecord(visitInfo);
            if (visitInfo.getStart() > 0) {
                skip(visitInfo, record, visitInfo.getStart());
                visitInfo.setRecordIndex(visitInfo.getStart());
            }
            int  cnt = 0;
            long t1  = System.currentTimeMillis();

            while (true) {
                if ((visitInfo.getStop() > 0)
                        && (visitInfo.getRecordIndex()
                            > visitInfo.getStop())) {
                    break;
                }
                try {
                    //This sets the record index
                    Record.ReadStatus status = readNextRecord(visitInfo,
                                                   record);
                    if (status == Record.ReadStatus.EOF) {
                        break;
                    }
                    record.index = visitInfo.getRecordIndex();
                    if (status == Record.ReadStatus.OK) {
                        if ( !processAfterReading(visitInfo, record)) {
                            continue;
                        }
                        if ((filter == null)
                                || filter.isRecordOk(record, visitInfo)) {
                            cnt++;
                            if ((visitInfo.getMax() > 0)
                                    && (cnt >= visitInfo.getMax())) {
                                break;
                            }
                            if ( !visitor.visitRecord(this, visitInfo,
                                    record)) {
                                break;
                            }
                        }
                    }

                    //System.err.println ("TIME:" + new Date(record.getRecordTime()));

                    record = makeNextRecord(record);
                    if (record == null) {
                        return;
                    }
                    skip = getSkip(visitInfo);
                    if (skip > 0) {
                        skip(visitInfo, record, skip);
                    }
                } catch (java.io.EOFException oef) {
                    //Bad form to catch an exception as logic but...
                    //                    oef.printStackTrace();
                    break;
                } catch (Exception exc) {
                    throw exc;
                }
            }
            if (ok) {
                visitorFinished(visitor, visitInfo);
            }
            long t2 = System.currentTimeMillis();
            if (debug) {
                System.err.println("RECORD: # visited:" + cnt + " in time:"
                                   + (t2 - t1) + "ms");
            }
        } finally {
            try {
                recordIO.close();
            } catch (Exception ignore) {}
        }
    }

    /**
     * _more_
     *
     * @param indices _more_
     */
    public void setYMDHMSIndices(int[] indices) {

        ymdhmsIndices = indices;
        if (ymdhmsIndices != null) {
            mySdfs = getDateFormat(ymdhmsIndices);
        }
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean processAfterReading(VisitInfo visitInfo, Record record)
            throws Exception {


        if (ymdhmsIndices != null) {
            setDateFromYMDHMS(record, ymdhmsIndices);
        } else if ((mySdfs != null)
                   && ((dateIndex != -1) || (timeIndex != -1))) {
            setDateFromDateAndTimeIndex(record);

        }

        return true;
    }

    /**
     * _more_
     *
     * @param visitor _more_
     * @param visitInfo _more_
     *
     * @throws Exception _more_
     */
    public void visitorFinished(RecordVisitor visitor, VisitInfo visitInfo)
            throws Exception {
        visitor.finished(this, visitInfo);
    }


    /**
     * _more_
     *
     *
     * @param pw _more_
     * @throws Exception On badness
     */
    public void printCsv(final PrintWriter pw) throws Exception {
        final int[]   cnt     = { 0 };
        RecordVisitor visitor = new RecordVisitor() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       Record record) {
                if (cnt[0] == 0) {
                    record.printCsvHeader(visitInfo, pw);
                }
                cnt[0]++;
                record.printCsv(visitInfo, pw);

                return true;
            }
        };
        visit(visitor);
    }


    /**
     * _more_
     *
     * @param pw _more_
     * @param fields _more_
     *
     * @throws Exception On badness
     */
    public void printCsv(final PrintWriter pw, final List<RecordField> fields)
            throws Exception {
        RecordVisitor visitor = new CsvVisitor(pw, fields);
        visit(visitor);
    }


    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param record _more_
     * @param howMany _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean skip(VisitInfo visitInfo, Record record, int howMany)
            throws Exception {
        visitInfo.addRecordIndex(howMany);
        //        System.err.println ("RecordFile.skip: io.skip= " + howMany);
        visitInfo.getRecordIO().getDataInputStream().skipBytes(howMany
                * record.getRecordSize());

        return true;
    }


    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Record getRecord(int index) throws Exception {
        RecordIO recordIO = doMakeInputIO(false);
        Record   record   = (Record) makeRecord(new VisitInfo());
        skip(new VisitInfo(recordIO), record, index);
        record.readNextRecord(recordIO);

        return record;
    }


    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordIO readHeader(RecordIO recordIO) throws Exception {
        return recordIO;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws Exception On badness
     */
    public void writeHeader(RecordIO recordIO) throws Exception {}


    /**
     * _more_
     *
     * @param recordOutput _more_
     * @param visitInfo _more_
     * @param filter _more_
     * @param writeHeader _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public VisitInfo write(RecordIO recordOutput, VisitInfo visitInfo,
                           RecordFilter filter, boolean writeHeader)
            throws Exception {
        if (visitInfo == null) {
            visitInfo = doMakeVisitInfo();
        }
        RecordIO recordInput = doMakeInputIO(getSkip(visitInfo) == 0);
        recordInput = readHeader(recordInput);
        visitInfo.setRecordIO(recordInput);
        if (writeHeader) {
            writeHeader(recordOutput);
        }
        try {
            writeRecords(recordInput, recordOutput, visitInfo, filter);
        } finally {
            try {
                recordInput.close();
            } catch (Exception ignore) {}
        }

        return visitInfo;
    }




    /**
     * _more_
     *
     * @param recordInput _more_
     * @param recordOutput _more_
     * @param visitInfo _more_
     * @param filter _more_
     *
     * @throws Exception On badness
     */
    public void writeRecords(RecordIO recordInput, RecordIO recordOutput,
                             VisitInfo visitInfo, RecordFilter filter)
            throws Exception {
        Record record = makeRecord(visitInfo);
        int    index  = 0;
        if (visitInfo.getStart() > 0) {
            skip(visitInfo, record, visitInfo.getStart());
            index = visitInfo.getStart();
        }
        long t1 = System.currentTimeMillis();
        while (true) {
            if ((visitInfo.getStop() > 0) && (index > visitInfo.getStop())) {
                break;
            }
            try {
                Record.ReadStatus status = record.readNextRecord(recordInput);
                if (status != Record.ReadStatus.OK) {
                    break;
                }

                record.index = index;
                if ((filter == null)
                        || filter.isRecordOk(record, visitInfo)) {
                    visitInfo.incrCount();
                    record.write(recordOutput);
                }
                record = makeNextRecord(record);
                if (record == null) {
                    return;
                }
                int skip = getSkip(visitInfo);

                if (skip > 0) {
                    skip(visitInfo, record, skip);
                    index += skip;
                } else {
                    index++;
                }
            } catch (java.io.EOFException oef) {
                //Bad form to catch an exception as logic but...
                break;
            }
        }
        long t2 = System.currentTimeMillis();
        //            System.err.println("Time:" + (t2 - t1));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return filename;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getFilename() {
        return filename;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getNormalizedFilename() {
        return Utils.normalizeTemplateUrl(filename);
    }



    /**
     * Set the filename
     *
     * @param filename filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * _more_
     *
     * @param skip _more_
     */
    public void setDefaultSkip(int skip) {
        defaultSkip = skip;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public long getNumRecords() {
        return numRecords;
    }


    /**
     * _more_
     *
     * @param n _more_
     */
    public void setNumRecords(long n) {
        numRecords = n;
    }




    /**
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            InputStream inputStream =
                new BufferedInputStream(new FileInputStream(arg), 1000000);
            DataInputStream dis = new DataInputStream(inputStream);

        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHtmlDescription() {
        return "";
    }

    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf;
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param v _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(Record record, RecordField field,
                                  double v) {
        double missing = field.getMissingValue();
        //        System.err.println("isMissing:" + v +" " + missing);
        return missing == v;
    }


    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param s _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(Record record, RecordField field,
                                  String s) {
        //I really shouldn't be doing this here
        return (s.length() == 0) || s.equals("---") || s.equals("n.v.")
               || s.equals("null") || s.equals("nan") || s.equals("NAN")
               || s.equals("NA") || s.equals("N/A") || s.equals("NaN")
               || s.equals("UKN") || s.equals("E");
    }


    /** _more_ */
    public static final String FILE_SEPARATOR = "_file_";

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getOriginalFilename(String name) {
        File f = new File(name);
        name = f.getName();
        int idx = name.indexOf(FILE_SEPARATOR);
        if (idx >= 0) {
            name = name.substring(idx + FILE_SEPARATOR.length());
        }

        return name;
    }



    /**
     * _more_
     *
     *
     * @param ymdhmsIndices _more_
     *
     * @return _more_
     */
    private SimpleDateFormat[] getDateFormat(int[] ymdhmsIndices) {
        int goodCnt = 0;
        for (int i = 0; i < ymdhmsIndices.length; i++) {
            if (ymdhmsIndices[i] < 0) {
                break;
            }
            goodCnt++;
        }

        if (goodCnt == 0) {
            return null;
        }

        return SDFS[goodCnt - 1];
    }


    /*
     * This gets called after a record has been read
     * It extracts and creates the record date/time
     */

    /**
     * _more_
     *
     * @param record _more_
     * @param indices _more_
     *
     * @throws Exception On badness
     */
    private void setDateFromYMDHMS(Record record, int[] indices)
            throws Exception {
        dttm.setLength(0);
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0) {
                break;
            }
            if (i > 0) {
                dttm.append("-");
            }
            dttm.append(getString(record, indices[i]));
        }
        setDate(record, dttm.toString());
    }



    /**
     * _more_
     *
     * @param record _more_
     *
     * @throws Exception On badness
     */
    public void setDateFromDateAndTimeIndex(Record record) throws Exception {
        dttm.setLength(0);
        getDateTimeString(record, dttm, dateIndex, timeIndex);

        setDate(record, dttm.toString());
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Date setDate(Record record, String dttm) throws Exception {
        if (mySdfs != null) {
            for (SimpleDateFormat sdf : mySdfs) {
                try {
                    Date date = sdf.parse(dttm);
                    record.setRecordTime(date.getTime());

                    return date;
                } catch (Exception exc) {
                    //                    System.err.println("Bad date:" + dttm + " " + exc);
                }
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param record _more_
     * @param dttm _more_
     * @param dateIndex _more_
     * @param timeIndex _more_
     *
     * @throws Exception _more_
     */
    public void getDateTimeString(Record record, StringBuffer dttm,
                                  int dateIndex, int timeIndex)
            throws Exception {
        if (dateIndex >= 0) {
            dttm.append(getString(record, dateIndex));
        }
        if (timeIndex >= 0) {
            if (dateIndex >= 0) {
                dttm.append(" ");
            }
            //Not sure if we want to pad as the date format might handle it
            String timeField = getString(record, timeIndex);
            if (timeField.length() == 1) {
                dttm.append("000");
            } else if (timeField.length() == 2) {
                dttm.append("00");
            } else if (timeField.length() == 3) {
                dttm.append("0");
            }
            dttm.append(timeField);
        }
    }


    /**
     * _more_
     *
     * @param record _more_
     * @param index _more_
     *
     * @return _more_
     */
    public String getString(Record record, int index) {
        if (record.hasObjectValue(index)) {
            return record.getObjectValue(index).toString();
        } else {
            int v = (int) record.getValue(index);

            return Integer.toString(v);
        }
    }



    /**
     *  Set the NameFromFile property.
     *
     *  @param value The new value for NameFromFile
     */
    public void setNameFromFile(String value) {
        nameFromFile = value;
    }

    /**
     *  Get the NameFromFile property.
     *
     *  @return The NameFromFile
     */
    public String getNameFromFile() {
        return nameFromFile;
    }

    /**
     *  Set the DescriptionFromFile property.
     *
     *  @param value The new value for DescriptionFromFile
     */
    public void setDescriptionFromFile(String value) {
        descriptionFromFile = value;
    }

    /**
     *  Get the DescriptionFromFile property.
     *
     *  @return The DescriptionFromFile
     */
    public String getDescriptionFromFile() {
        return descriptionFromFile;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getFileProperties() {
        return fileProperties;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void putFileProperty(String name, Object value) {
        fileProperties.put(name, value);
    }


    /**
     * _more_
     *
     * @param dateIndex _more_
     * @param timeIndex _more_
     */
    public void setDateTimeIndex(int dateIndex, int timeIndex) {
        this.dateIndex = dateIndex;
        this.timeIndex = timeIndex;
        String pattern = "yyyy-MM-dd";
        if (timeIndex >= 0) {
            pattern += " HHmm";
        }
        mySdfs = new SimpleDateFormat[] {
            makeDateFormat(getProperty(PROP_DATEFORMAT, pattern)) };


    }



    /**
     * Set the Sdf property.
     *
     * @param value The new value for Sdf
     */
    public void setSdf(SimpleDateFormat value) {
        mySdfs = new SimpleDateFormat[] { value };
    }

    /**
     * Get the Sdf property.
     *
     * @return The Sdf
     */
    public SimpleDateFormat[] getSdf() {
        return mySdfs;
    }

    /**
     *  Set the DateIndex property.
     *
     *  @param value The new value for DateIndex
     */
    public void setDateIndex(int value) {
        dateIndex = value;
    }

    /**
     *  Get the DateIndex property.
     *
     *  @return The DateIndex
     */
    public int getDateIndex() {
        return dateIndex;
    }

    /**
     *  Set the TimeIndex property.
     *
     *  @param value The new value for TimeIndex
     */
    public void setTimeIndex(int value) {
        timeIndex = value;
    }

    /**
     *  Get the TimeIndex property.
     *
     *  @return The TimeIndex
     */
    public int getTimeIndex() {
        return timeIndex;
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String cleanFieldName(String s) {
        s = s.toLowerCase();
        s = s.replaceAll("\\s+", "_");

        return s;
    }


}
