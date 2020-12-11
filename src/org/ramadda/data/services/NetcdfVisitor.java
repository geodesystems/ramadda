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
import org.ramadda.data.record.*;

import org.ramadda.repository.*;
import org.ramadda.repository.job.*;
import org.ramadda.util.Utils;

import ucar.ma2.DataType;


import ucar.ma2.StructureMembers;

import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleAdapter;
import ucar.nc2.VariableSimpleIF;
//import ucar.nc2.ft.point.writer.CFPointObWriter;
//import ucar.nc2.ft.point.writer.PointObVar;
import ucar.nc2.dt.point.CFPointObWriter;
import ucar.nc2.dt.point.PointObVar;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.units.DateUnit;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 */
public class NetcdfVisitor extends BridgeRecordVisitor {

    /** _more_ */
    public static final int MAX_STRING_LENGTH = 100;

    /** _more_ */
    private int recordCnt = 0;

    /** _more_ */
    private PointDataRecord cacheRecord;

    /** _more_ */
    private List<VariableSimpleIF> dataVars;

    /** _more_ */
    private File tmpFile;

    /** _more_ */
    private File outputNetcdfFile;

    /** _more_ */
    private RecordIO tmpFileIO;

    /** _more_ */
    private WriterCFPointCollection writer;

    /** _more_ */
    private CsvVisitor csvVisitor = null;

    /** _more_ */
    private List<RecordField> fields;

    /** _more_ */
    private double[] dvals;

    /** _more_ */
    private String[] svals;

    /** _more_ */
    private boolean hasTime = false;

    /** _more_ */
    private Date now;

    /** _more_ */
    int cnt = 0;


    /**
     * _more_
     *
     * @param handler _more_
     * @param request _more_
     * @param processId _more_
     * @param mainEntry _more_
     */
    public NetcdfVisitor(RecordOutputHandler handler, Request request,
                         Object processId, Entry mainEntry) {
        super(handler, request, processId, mainEntry, ".nc");
    }

    /**
     * _more_
     *
     * @param tmpFile _more_
     * @param outputNetcdfFile _more_
     */
    public NetcdfVisitor(File tmpFile, File outputNetcdfFile) {
        this.tmpFile          = tmpFile;
        this.outputNetcdfFile = outputNetcdfFile;
    }



    /**
     * _more_
     *
     * @param file _more_
     * @param record _more_
     *
     * @throws Exception _more_
     */
    private void init(RecordFile file, BaseRecord record) throws Exception {
        now     = new Date();
        hasTime = record.hasRecordTime();
        fields  = new ArrayList<RecordField>();
        List<VariableSimpleIF> stringVars = new ArrayList<VariableSimpleIF>();
        dataVars = new ArrayList<VariableSimpleIF>();
        int              numDouble        = 0;
        int              numString        = 0;
        StructureMembers structureMembers = new StructureMembers("point obs");
        for (RecordField field : file.getFields()) {
            if ( !(field.isTypeNumeric() || field.isTypeString())) {
                continue;
            }
            //Having a field called time breaks the cfwriter
            if (field.getName().equals("time")) {
                System.err.println("****SKIPPING TIME FIELD ********");

                continue;
            }


            fields.add(field);
            String unit = field.getUnit();
            if (unit == null) {
                unit = "";
            }
            String desc = field.getDescription();
            if ( !Utils.stringDefined(desc)) {
                desc = field.getName();
            }
            System.err.println("name:" + field.getName());
            System.err.println("desc:" + desc);
            System.err.println("unit:" + unit);
            VariableSimpleAdapter pointObVar = new VariableSimpleAdapter(
                                                   structureMembers.addMember(
                                                       field.getName(), desc,
                                                       unit,
                                                       field.isTypeNumeric()
                    ? DataType.DOUBLE
                    : DataType.STRING, new int[] { 1 }));

            if (field.isTypeNumeric()) {
                numDouble++;
                dataVars.add(pointObVar);
            } else if (field.isTypeString()) {
                numString++;
                //                pointObVar.setLen(MAX_STRING_LENGTH);
                stringVars.add(pointObVar);
            }
            if (true) {
                break;
            }
        }
        dataVars.addAll(stringVars);
        dvals       = new double[numDouble];
        svals       = new String[numString];
        cacheRecord = new PointDataRecord((RecordFile) null);
        if (tmpFile == null) {
            tmpFile = getHandler().getStorageManager().getTmpFile(null,
                    "tmp.nc");
        }
        tmpFileIO             = new RecordIO(new FileOutputStream(tmpFile));
        cacheRecord.dvalsSize = dvals.length;
        cacheRecord.svalsSize = svals.length;



        VariableSimpleAdapter var =
            new VariableSimpleAdapter(structureMembers.addMember("test",
                "test", "m", DataType.DOUBLE, new int[] { 1 }));
        List<VariableSimpleIF> vars = new ArrayList<VariableSimpleIF>();
        vars.add(var);

        /*
        writer = new WriterCFPointCollection("test.nc", "point data");
        writer.writeHeader(vars, DateUnit.getUnixDateUnit(), "m");
        writer.finish();
        */


        /*
        writer = new WriterCFPointCollection(outputNetcdfFile.toString(), "point data");
        writer.writeHeader(dataVars, DateUnit.getUnixDateUnit(), "m");
        writer.finish();
        */

    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean jobOK() {
        Object jobId = getProcessId();
        if ((jobId != null) && (getHandler() != null)) {
            return getHandler().jobOK(jobId);
        }

        return true;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean doVisitRecord(RecordFile file, VisitInfo visitInfo,
                                 BaseRecord record)
            throws Exception {
        if (tmpFileIO == null) {
            init(file, record);
        }
        if ( !jobOK()) {
            return false;
        }
        int         dcnt        = 0;
        int         scnt        = 0;
        PointRecord pointRecord = (PointRecord) record;
        for (RecordField field : fields) {
            if (field.isTypeNumeric()) {
                dvals[dcnt++] = record.getValue(field.getParamId());
            } else if (field.isTypeString()) {
                String s = record.getStringValue(field.getParamId());
                if (s.length() > MAX_STRING_LENGTH) {
                    s = s.substring(0, MAX_STRING_LENGTH);
                }
                svals[scnt++] = s;
            }
        }
        recordCnt++;
        cacheRecord.setLatitude(pointRecord.getLatitude());
        cacheRecord.setLongitude(pointRecord.getLongitude());
        cacheRecord.setAltitude(pointRecord.getAltitude());
        if (hasTime) {
            cacheRecord.setTime(record.getRecordTime());
        } else {
            cacheRecord.setTime(now.getTime());
        }
        cacheRecord.setDvals(dvals);
        cacheRecord.setSvals(svals);
        cacheRecord.write(tmpFileIO);

        return true;
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     */
    @Override
    public void close(VisitInfo visitInfo) {
        try {
            if (tmpFileIO == null) {
                return;
            }
            tmpFileIO.close();
            List<Attribute>  globalAttributes = new ArrayList<Attribute>();
            DataOutputStream dos;
            if (outputNetcdfFile != null) {
                //                dos = new DataOutputStream(new FileOutputStream(outputNetcdfFile));
            } else {
                //TODO: 
                dos = getTheDataOutputStream();
            }
            System.err.println("data var:" + dataVars.size());
            //            writer = new CFPointObWriter(dos, globalAttributes, "m", dataVars, recordCnt);
            tmpFileIO = new RecordIO(new FileInputStream(tmpFile));
            System.err.println("Point.NetcdfVisitor:writing # records:"
                               + recordCnt + "\n\t #dvals:"
                               + cacheRecord.getDvals().length
                               + "\n\t #svals:"
                               + cacheRecord.getSvals().length
                               + " \n\t dataVars:" + dataVars.size());

            /*
            for (int i = 0; i < recordCnt; i++) {
                cacheRecord.read(tmpFileIO);
                writer.addPoint(cacheRecord.getLatitude(),
                                cacheRecord.getLongitude(),
                                cacheRecord.getAltitude(),
                                new Date(cacheRecord.getTime()),
                                cacheRecord.getDvals(),
                                cacheRecord.getSvals());
            }
            writer.finish();
            */
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        super.close(visitInfo);
    }
}
