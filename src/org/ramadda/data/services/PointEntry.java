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


import org.ramadda.data.point.*;
import org.ramadda.data.point.binary.*;


import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;


import org.ramadda.repository.*;

import ucar.unidata.util.IOUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * This is a wrapper around  ramadda Entry and a RecordFile
 *
 *
 */
public class PointEntry extends RecordEntry {

    /** _more_ */
    public static final String SUFFIX_BINARY_DOUBLE = ".dllb";

    /** _more_ */
    public static final String SUFFIX_BINARY_FLOAT = ".fllb";

    /** _more_ */
    public static final String FILE_BINARY_DOUBLE = "lightweight"
                                                    + SUFFIX_BINARY_DOUBLE;

    /** _more_ */
    public static final String FILE_BINARY_FLOAT = "lightweight"
                                                   + SUFFIX_BINARY_FLOAT;

    /** _more_ */
    public static final String FILE_BINARY_DEFAULT = FILE_BINARY_FLOAT;



    /** This points to the  short lat/lon/alt binary file ramadda creates on the fly */
    private PointFile binaryPointFile;



    /**
     * ctor
     *
     * @param outputHandler _more_
     * @param request the request
     * @param entry the entry
     */
    public PointEntry(PointOutputHandler outputHandler, Request request,
                      Entry entry) {
        super(outputHandler, request, entry);
    }



    /**
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
    public static List<PointEntry> toPointEntryList(List l) {
        List<PointEntry> pointEntries = new ArrayList<PointEntry>();
        for (Object o : l) {
            pointEntries.add((PointEntry) o);
        }

        return pointEntries;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public PointOutputHandler getPointOutputHandler() {
        return (PointOutputHandler) getOutputHandler();
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public PointFile getPointFile() throws Exception {
        return (PointFile) getRecordFile();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile getRecordFile() throws Exception {
        RecordFile recordFile = (RecordFile) super.getRecordFile();
        if (recordFile == null) {
            long records = getNumRecordsFromEntry(-1);
            //            System.err.println ("PointEntry.getRecordFile");
            recordFile =
                getPointOutputHandler().createAndInitializeRecordFile(
                    getRequest(), getEntry(), records);
            setRecordFile(recordFile);
        }

        return recordFile;
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public static boolean isDoubleBinaryFile(File f) {
        return f.toString().endsWith(SUFFIX_BINARY_DOUBLE);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isArealCoverage() throws Exception {
        return getRecordFile().isCapable(PointFile.ACTION_AREAL_COVERAGE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File getQuickScanFile() {
        File entryDir = getOutputHandler().getStorageManager().getEntryDir(
                            getEntry().getId(), true);
        //Look for one that exists
        for (String file : new String[] { FILE_BINARY_DOUBLE,
                                          FILE_BINARY_FLOAT }) {
            File quickscanFile = new File(IOUtil.joinDir(entryDir, file));
            if (quickscanFile.exists()) {
                return quickscanFile;
            }
        }

        return new File(IOUtil.joinDir(entryDir, FILE_BINARY_DEFAULT));
    }



    /**
     * apply the visitor to the point file
     *
     * @param visitor visitor
     * @param visitInfo visit info
     *
     * @throws Exception On badness
     */
    @Override
    public void visit(RecordVisitor visitor, VisitInfo visitInfo)
            throws Exception {
        if ((visitInfo != null) && visitInfo.getQuickScan()) {
            PointFile quickscanFile = getBinaryPointFile();
            System.err.println("POINT: Using quick scan file #records = "
                               + quickscanFile.getNumRecords());
            quickscanFile.visit(visitor, visitInfo, getFilter());

            return;
        }
        super.visit(visitor, visitInfo);
    }



    /**
     * get the Point File for the short lat/lon/alt binary file
     *
     * @return short binary file
     *
     * @throws Exception On badness
     */
    public PointFile getBinaryPointFile() throws Exception {
        PointFile pointFile = getPointFile();
        if (pointFile instanceof DoubleLatLonBinaryFile) {
            return pointFile;
        }
        if (pointFile instanceof FloatLatLonBinaryFile) {
            return pointFile;
        }
        if (binaryPointFile == null) {
            File entryDir =
                getOutputHandler().getStorageManager().getEntryDir(
                    getEntry().getId(), true);
            File quickscanFile = getQuickScanFile();
            if ( !quickscanFile.exists()) {
                //Write to a tmp file and only move it over when we are done
                File tmpFile = new File(IOUtil.joinDir(entryDir,
                                   "tmpfile.bin"));
                pointFile.setDefaultSkip(0);
                System.err.println("POINT: making quickscan file ");
                writeBinaryFile(tmpFile, pointFile,
                                isDoubleBinaryFile(quickscanFile));
                tmpFile.renameTo(quickscanFile);
                System.err.println("POINT: done making quickscan file");
            }

            if (isDoubleBinaryFile(quickscanFile)) {
                binaryPointFile =
                    new DoubleLatLonBinaryFile(quickscanFile.toString());
            } else {
                binaryPointFile =
                    new FloatLatLonBinaryFile(quickscanFile.toString());
            }
        }

        return binaryPointFile;
    }


    /**
     * _more_
     *
     * @param outputFile file to write to
     * @param pointFile file to read from
     * @param asDouble _more_
     *
     * @throws Exception On badness
     */
    public void writeBinaryFile(File outputFile, PointFile pointFile,
                                boolean asDouble)
            throws Exception {

        if (asDouble) {
            DoubleLatLonBinaryFile
                .writeBinaryFile(pointFile, getPointOutputHandler()
                    .getStorageManager()
                    .getUncheckedFileOutputStream(outputFile), null);
        } else {
            FloatLatLonBinaryFile
                .writeBinaryFile(pointFile, getPointOutputHandler()
                    .getStorageManager()
                    .getUncheckedFileOutputStream(outputFile), null);
        }
    }



}
