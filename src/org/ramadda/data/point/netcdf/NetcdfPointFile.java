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

package org.ramadda.data.point.netcdf;


import org.ramadda.data.point.*;


import org.ramadda.data.record.*;
import org.ramadda.data.services.*;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.util.Utils;

import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.List;



/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class NetcdfPointFile extends PointFile {

    /** _more_ */
    public static final String NETCDF_ATTR_SUMMARY = "summary";



    /**
     * ctor
     */
    public NetcdfPointFile() {}




    /**
     * ctor
     *
     * @param filename _more_
     * @throws IOException On badness
     */
    public NetcdfPointFile(String filename) throws IOException {
        super(filename);
    }


    /**
     * ctor
     *
     * @param filename _more_
     * @param properties _more_
     * @throws IOException On badness
     */
    public NetcdfPointFile(String filename, Hashtable properties)
            throws IOException {
        super(filename, properties);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getFileToUse() throws Exception {
        String filename = getFilename();

        Entry  entry    = null;
        RecordEntry recordEntry =
            (RecordEntry) getProperty("prop.recordentry");
        if (recordEntry != null) {
            entry = recordEntry.getEntry();
        } else {
            entry = (Entry) getProperty("entry");
        }
        if (entry == null) {
            return filename;
        }
        Repository repository = entry.getTypeHandler().getRepository();

        List<Metadata> metadataList =
            repository.getMetadataManager().findMetadata(
                repository.getTmpRequest(), entry,
                ContentMetadataHandler.TYPE_ATTACHMENT, true);
        if (metadataList == null) {
            return filename;
        }
        for (Metadata metadata : metadataList) {
            String fileAttachment = metadata.getAttr1();
            if ( !fileAttachment.endsWith(".ncml")) {
                continue;
            }
            File templateNcmlFile =
                new File(
                    IOUtil.joinDir(
                        repository.getStorageManager().getEntryDir(
                            metadata.getEntryId(),
                            false), metadata.getAttr1()));
            String ncml = repository.getStorageManager().readSystemResource(
                              templateNcmlFile).replace(
                              "${location}", filename);
            String dttm = templateNcmlFile.lastModified() + "";
            String fileName = dttm + "_" + entry.getId() + "_"
                              + metadata.getId() + ".ncml";
            File ncmlFile =
                repository.getStorageManager().getScratchFile(fileName);
            IOUtil.writeBytes(ncmlFile, ncml.getBytes());
            filename = ncmlFile.toString();

            break;
        }

        return filename;
    }




    /**
     * _more_
     *
     *
     * @param failureOk _more_
     * @return _more_
     */
    @Override
    public List<RecordField> doMakeFields(boolean failureOk) {

        Hashtable<String, RecordField> dfltFields = new Hashtable<String,
                                                        RecordField>();
        String fieldsProperty = getProperty("fields", "NONE");
        boolean defaultChartable = getProperty("chartable",
                                       "true").equals("true");
        if (fieldsProperty != null) {
            List<RecordField> fields = doMakeFields(fieldsProperty);
            for (RecordField field : fields) {
                if (field.getChartable()) {
                    //don't do this for now
                    //                    defaultChartable = false;
                }
                dfltFields.put(field.getName(), field);
            }
        }
        List<RecordField> fields = new ArrayList<RecordField>();
        try {
            int cnt = 1;
            fields.add(new RecordField("latitude", "Latitude", "Latitude",
                                       cnt++, "degrees"));
            fields.add(new RecordField("longitude", "Longitude", "Longitude",
                                       cnt++, "degrees"));

            RecordField dateField = new RecordField("date", "Date", "Date",
                                        cnt++, "");
            dateField.setType(dateField.TYPE_DATE);
            fields.add(dateField);


            FeatureDatasetPoint pod  = getDataset(getFileToUse());
            List                vars = pod.getDataVariables();
            for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
                String label = var.getDescription();
                if ( !Utils.stringDefined(label)) {
                    label = var.getShortName();
                }
                String      unit  = var.getUnitsString();
                RecordField field = dfltFields.get(var.getShortName());
                if (field == null) {
                    field = new RecordField(var.getShortName(), label, label,
                                            cnt++, unit);
                    if ((var.getDataType() == DataType.STRING)
                            || (var.getDataType() == DataType.CHAR)) {
                        field.setType(field.TYPE_STRING);
                    } else {
                        field.setChartable(defaultChartable);
                        field.setSearchable(true);
                    }
                } else {
                    //                    System.err.println ("got default: " + field);
                }
                fields.add(field);
            }
            //            System.err.println ("fields: " + fields);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        return fields;
    }

    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static PointFeatureIterator getPointIterator(
            FeatureDatasetPoint input)
            throws Exception {
        List<FeatureCollection> collectionList =
            input.getPointFeatureCollectionList();
        if (collectionList.size() > 1) {
            throw new IllegalArgumentException(
                "Can't handle point data with multiple collections");
        }
        FeatureCollection      fc         = collectionList.get(0);
        PointFeatureCollection collection = null;
        if (fc instanceof PointFeatureCollection) {
            collection = (PointFeatureCollection) fc;
        } else if (fc instanceof NestedPointFeatureCollection) {
            NestedPointFeatureCollection npfc =
                (NestedPointFeatureCollection) fc;
            collection = npfc.flatten(null, (CalendarDateRange) null);
        } else {
            throw new IllegalArgumentException(
                "Can't handle collection of type " + fc.getClass().getName());
        }

        return collection.getPointFeatureIterator(16384);
    }



    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    @Override
    public boolean isCapable(String action) {
        if (action.equals(ACTION_TRACKS)) {
            return false;
        }
        if (action.equals(ACTION_MAPINCHART)) {
            return true;
        }

        //        if(action.equals(ACTION_BOUNDINGPOLYGON)) return false;
        return super.isCapable(action);
    }



    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean canLoad(String file) {
        if (true) {
            return false;
        }
        try {
            return file.endsWith(".nc");
        } catch (Exception exc) {
            return false;
        }
    }



    /**
     * This just passes through to FileType.doMakeRecord
     *
     *
     * @param visitInfo the visit info
     * @return the new record
     */
    public Record doMakeRecord(VisitInfo visitInfo) {
        try {
            FeatureDatasetPoint pod = getDataset(getFileToUse());
            if (pod == null) {
                throw new IllegalArgumentException(
                    "Given file is not a recognized point data file");
            }
            PointFeatureIterator dataIterator = getPointIterator(pod);
            NetcdfRecord record = new NetcdfRecord(this, getFields(),
                                      dataIterator);

            return record;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     * @param howMany _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    @Override
    public boolean skip(VisitInfo visitInfo, Record record, int howMany)
            throws Exception {
        visitInfo.addRecordIndex(howMany);
        while (howMany-- >= 0) {
            if (record.read(visitInfo.getRecordIO())
                    == Record.ReadStatus.EOF) {
                return false;
            }
        }

        return true;
    }




    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        visitInfo.setRecordIO(readHeader(visitInfo.getRecordIO()));


        NetcdfDataset   dataset  = NetcdfDataset.openDataset(getFileToUse());
        String          platform = "";

        List<Attribute> attrs    = dataset.getGlobalAttributes();
        for (Attribute attr : attrs) {
            String name  = attr.getName();
            String value = attr.getStringValue();
            if (value == null) {
                continue;
            }
            if (name.equals(NETCDF_ATTR_SUMMARY)) {
                setDescriptionFromFile(value);
            } else {
                putFileProperty(name, value);
            }
        }
        dataset.close();

        return visitInfo;
    }


    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public RecordIO readHeader(RecordIO recordIO) throws IOException {
        //        recordIO.getDataInputStream().read(header);
        return recordIO;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public long getNumRecords() {
        if (super.getNumRecords() <= 0) {
            try {
                RecordCountVisitor visitor = new RecordCountVisitor();
                visit(visitor, new VisitInfo(true), null);
                setNumRecords(visitor.getCount());
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return super.getNumRecords();
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, NetcdfPointFile.class);
    }



    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private FeatureDatasetPoint getDataset(String path) throws Exception {
        //        System.err.println("Opening:" + path);
        Formatter buf = new Formatter();
        FeatureDatasetPoint pods =
            (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.POINT, path, null, buf);
        if (pods == null) {  // try as ANY_POINT
            pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.ANY_POINT, path, null, buf);
        }
        if (pods == null) {}

        return pods;
    }






}
