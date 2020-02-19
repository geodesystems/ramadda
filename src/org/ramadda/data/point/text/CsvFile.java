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

package org.ramadda.data.point.text;


import org.ramadda.data.point.*;
import org.ramadda.data.record.*;

import org.ramadda.util.text.CsvUtil;

import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.image.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * CSV file supports any form of column delimited files - comma, tab, space, etc
 *
 */
public class CsvFile extends TextFile {


    /** column delimiter */
    private String delimiter = null;


    /**
     * ctor
     */
    public CsvFile() {}


    /**
     * ctor
     *
     *
     * @param filename _more_
     *
     * @throws IOException on badness
     */
    public CsvFile(String filename) throws IOException {
        super(filename);
    }

    /**
     * ctor
     *
     * @param filename _more_
     * @param properties _more_
     *
     * @throws IOException on badness
     */
    public CsvFile(String filename, Hashtable properties) throws IOException {
        super(filename, properties);
    }


    /**
     * _more_
     *
     * @param filename _more_
     * @param context _more_
     * @param properties _more_
     */
    public CsvFile(String filename, RecordFileContext context,
                   Hashtable properties) {
        super(filename, context, properties);
    }

    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     *
     * @throws Exception _more_
     */
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
        String csvCommands = getProperty("point.csvcommands", (String) null);
        if (csvCommands == null) {
            return super.doMakeInputStream(buffered);
        }
        File file = getCacheFile();
        //      System.err.println("file:" +file);
        if ((file == null) || !file.exists()) {
            try {
                ByteArrayOutputStream bos = null;
                OutputStream          fos;
                if (file != null) {
                    fos = new FileOutputStream(file);
                } else {
                    fos = bos = new ByteArrayOutputStream();
                }
                String[] args = StringUtil.listToStringArray(
                                    StringUtil.split(csvCommands, ","));
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].replaceAll("_comma_", ",");
                }
                CsvUtil csvUtil = new CsvUtil(args,
                                      new BufferedOutputStream(fos), null);
		System.err.println("csvutil");
                csvUtil.setInputStream(super.doMakeInputStream(buffered));
		System.err.println("csvutil run");
                csvUtil.run(null);
                fos.close();
                if (file == null) {
                    //                    System.err.println("processed:" +new String(bos.toByteArray()));
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return new BufferedInputStream(new FileInputStream(file));
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
     * _more_
     *
     * @param failureOk _more_
     *
     * @return _more_
     */
    @Override
    public List<RecordField> doMakeFields(boolean failureOk) {
        String fieldString = getProperty(PROP_FIELDS, null);
        //      System.err.println("CsvFile.doMakeFields props:" + getProperties());
        if (fieldString == null) {
            doQuickVisit();
            fieldString = getProperty(PROP_FIELDS, null);
        } else {
            //Read the header because there are properties
            if (getProperty(PROP_HEADER_STANDARD, false)) {
                doQuickVisit();
            }
        }


        commentLineStart = getProperty("commentLineStart", null);


        if (fieldString == null) {
            setIsHeaderStandard(true);
            doQuickVisit();
            fieldString = getProperty(PROP_FIELDS, null);
        }

        if (fieldString == null) {
            if (failureOk) {
                return new ArrayList<RecordField>();
            }

            throw new IllegalArgumentException("Properties must have a "
                    + PROP_FIELDS + " value");
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
    public Record doMakeRecord(VisitInfo visitInfo) {
        TextRecord record = new TextRecord(this, getFields());
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
            String arg = args[argIdx];
            try {
                long                t1       = System.currentTimeMillis();
                final int[]         cnt      = { 0 };
                CsvFile             file     = new CsvFile(arg);
                final RecordVisitor metadata = new RecordVisitor() {
                    public boolean visitRecord(RecordFile file,
                            VisitInfo visitInfo, Record record) {
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
