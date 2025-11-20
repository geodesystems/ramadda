/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.tools;


import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;

import ucar.unidata.util.Misc;

import java.io.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class RecordTool {

    /** _more_ */
    private RecordFileFactory recordFileFactory;

    /** _more_ */
    private String recordFileClass;

    /**
     * _more_
     *
     * @param fileFactoryClass _more_
     *
     * @throws Exception _more_
     */
    public RecordTool(String fileFactoryClass) throws Exception {
        if (fileFactoryClass != null) {
            recordFileFactory = (RecordFileFactory) Misc.findClass(
                fileFactoryClass).getDeclaredConstructor().newInstance();
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public List<String> processArgs(String[] args) {
        List<String> rest = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-file")) {
                recordFileClass = args[++i];

                continue;
            }


            if (arg.equals("-class")) {
                if (i == args.length - 1) {
                    usage("Need " + arg + " argument");
                }
                setRecordFileClass(args[++i]);

                continue;
            }
            rest.add(arg);
        }

        return rest;
    }





    /**
     * _more_
     *
     * @param inFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFile doMakeRecordFile(String inFile) throws Exception {

        if (recordFileClass == null) {
            recordFileClass = System.getenv("ramadda_point_class");
        }

        if (recordFileClass == null) {
            recordFileClass = System.getProperty("ramadda_point_class");
        }


        if ((recordFileClass == null) && (getRecordFileFactory() == null)) {
            if (inFile.endsWith(".txt") || inFile.endsWith(".csv")) {
                setRecordFileClass("org.ramadda.data.point.text.CsvFile");
            }
        }


        if (recordFileClass != null) {
            Class c = Misc.findClass(recordFileClass);
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { String.class });

            Hashtable properties = RecordFile.getPropertiesForFile(inFile,
                                       PointFile.DFLT_PROPERTIES_FILE);

            RecordFile recordFile =
                (RecordFile) ctor.newInstance(new Object[] { inFile });
            if (properties != null) {
                recordFile.setProperties(properties);
            }

            return recordFile;
        }
        if (getRecordFileFactory() == null) {
            throw new IllegalStateException(
                "No record file or record file factory specified\nUse -file <record file class>");
        }

        return getRecordFileFactory().doMakeRecordFile(inFile);
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public void usage(String message) {
        System.err.println("Error:" + message);
        org.ramadda.util.Utils.exitTest(1);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFileFactory getRecordFileFactory() throws Exception {
        return recordFileFactory;
    }

    /**
     * Set the RecordFileClass property.
     *
     * @param value The new value for RecordFileClass
     */
    public void setRecordFileClass(String value) {
        recordFileClass = value;
    }

    /**
     * Get the RecordFileClass property.
     *
     * @return The RecordFileClass
     */
    public String getRecordFileClass() {
        return recordFileClass;
    }



}
