/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.data.record;


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
//import ucar.nc2.dt.point.PointObVar;
//import ucar.nc2.dt.point.CFPointObWriter;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.units.DateUnit;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 */

@SuppressWarnings("deprecation")
public class NetcdfVisitor extends RecordVisitor {

    private static final boolean debug = false;

    /** _more_ */
    public static final int MAX_STRING_LENGTH = 200;

    /** _more_ */
    private int recordCnt = 0;

    /** _more_ */
    private PointDataRecord cacheRecord;

    /** _more_ */
    //    private List<VariableSimpleIF> dataVars;
    private List<ucar.nc2.dt.point.PointObVar> dataVars;


    /** _more_ */
    private List<RecordField> fields;

    /** _more_ */
    private File tmpFile;

    /** _more_ */
    private File outputNetcdfFile;

    /** _more_ */
    private RecordIO tmpFileIO;

    /** _more_ */
    //    private WriterCFPointCollection writer;

    private DataOutputStream dos;

    /** _more_ */
    private double[] dvals;

    /** _more_ */
    private String[] svals;

    /** _more_ */
    private boolean hasTime = false;

    /** _more_ */
    private Date now;

    /**
     * _more_
     *
     * @param tmpFile _more_
     * @param outputNetcdfFile _more_
     */
    public NetcdfVisitor(File tmpFile, File outputNetcdfFile,List<RecordField> subsetFields) {
        this.tmpFile          = tmpFile;
        this.outputNetcdfFile = outputNetcdfFile;
	this.fields = subsetFields;
    }

    public NetcdfVisitor(File tmpFile, DataOutputStream dos,List<RecordField> subsetFields) {
        this.tmpFile          = tmpFile;
        this.dos= dos;
	this.fields = subsetFields;
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
	//        List<VariableSimpleIF> stringVars = new ArrayList<VariableSimpleIF>();
        List<ucar.nc2.dt.point.PointObVar> stringVars = new ArrayList<>();	
	dataVars = new ArrayList<>();
        int              numDouble        = 0;
        int              numString        = 0;
	List<RecordField> tmpFields = new ArrayList<RecordField>();
	if(fields==null) fields = file.getFields();
        for (RecordField field : fields) {
            if ( !(field.isTypeNumeric() || field.isTypeString())) {
                continue;
            }
            //Having a field called time breaks the cfwriter
            if (field.getName().equals("time")) {
                System.err.println("****SKIPPING TIME FIELD ********");
                continue;
            }

            tmpFields.add(field);
	}
	fields = tmpFields;
	for(RecordField field: fields) {
            String unit = field.getUnit();
            if (unit == null) {
                unit = "";
            }
            String desc = field.getDescription();
            if ( !Utils.stringDefined(desc)) {
                desc = field.getName();
            }
	    ucar.nc2.dt.point.PointObVar pointObVar = new ucar.nc2.dt.point.PointObVar();
	    String name = field.getName();
	    //the nc writer chokes if there is a field called record
	    if(name.equals("record")) name = "datarecord";
	    pointObVar.setName(name);
	    pointObVar.setUnits(unit);
	    if(field.isTypeNumeric())
		pointObVar.setDataType(DataType.DOUBLE);
	    else
		pointObVar.setDataType(DataType.STRING);
/*
  StructureMembers structureMembers = new StructureMembers("point obs");
            VariableSimpleAdapter pointObVar = new VariableSimpleAdapter(
                                                   structureMembers.addMember(
                                                       field.getName(), desc,
                                                       unit,
                                                       field.isTypeNumeric()
                    ? DataType.DOUBLE
                    : DataType.STRING, new int[] { 1 }));
*/

            if (field.isTypeNumeric()) {
                numDouble++;
		dataVars.add(pointObVar);
            } else if (field.isTypeString()) {
                numString++;
		pointObVar.setLen(MAX_STRING_LENGTH);
		stringVars.add(pointObVar);
            }
	}
        dataVars.addAll(stringVars);

        dvals       = new double[numDouble];
        svals       = new String[numString];
        cacheRecord = new PointDataRecord((RecordFile) null);
        tmpFileIO             = new RecordIO(new FileOutputStream(tmpFile));
        cacheRecord.dvalsSize = dvals.length;
        cacheRecord.svalsSize = svals.length;

	/*
	//        writer = new WriterCFPointCollection("test.nc", "point data");
	writer = new ucar.nc2.dt.point.CFPointObWriter(dos, attrs, ((alt != null)
	? alt.getUnit().toString()
	: null), dataVars, obs.size());
	writer.writeHeader(vars, DateUnit.getUnixDateUnit(), "m");
        writer.finish();
	*/
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
    public boolean visitRecord(RecordFile file, VisitInfo visitInfo, BaseRecord record)
            throws Exception {
        if (tmpFileIO == null) {
            init(file, record);
        }
	//	if(recordCnt>3) return false;
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
        cacheRecord.setDoubleValues(dvals);
        cacheRecord.setStringValues(svals);
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
	    if(dos==null) {
		if (outputNetcdfFile != null) {
		    dos = new DataOutputStream(new FileOutputStream(outputNetcdfFile));
		} else {
		    throw new IllegalStateException("NetcdfVisitor:no data output stream provided");
		}
	    }
	    ucar.nc2.dt.point.CFPointObWriter  writer = 
		new ucar.nc2.dt.point.CFPointObWriter(dos, globalAttributes, "m", dataVars, recordCnt);
            tmpFileIO = new RecordIO(new FileInputStream(tmpFile));
	    if(debug)
		System.err.println("Point.NetcdfVisitor:writing # records:"
				   + recordCnt + "\n\t #dvals:"
				   + cacheRecord.getDoubleValues().length
				   + "\n\t #svals:"
				   + cacheRecord.getStringValues().length
				   + " \n\t dataVars:" + dataVars.size());

            for (int i = 0; i < recordCnt; i++) {
                cacheRecord.read(tmpFileIO);
		//If we write out lat/lon as NaN then when reading the generated nc file it fails
		//So we write out -9999 to sign
		writer.addPoint(Double.isNaN(cacheRecord.getLatitude())?-9999:cacheRecord.getLatitude(),
				Double.isNaN(cacheRecord.getLongitude())?-9999:cacheRecord.getLongitude(),
				Double.isNaN(cacheRecord.getAltitude())?-9999:cacheRecord.getAltitude(),
                                new Date(cacheRecord.getTime()),
                                cacheRecord.getDoubleValues(),
                                cacheRecord.getStringValues());

            }
            writer.finish();
            if (outputNetcdfFile != null) {
		dos.close();
	    }
	    tmpFile.delete();
		
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        super.close(visitInfo);
    }
}
