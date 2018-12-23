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

import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.*;
import ucar.nc2.ft.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;




/**
 * Class description
 *
 *
 * @version        $version$, Thu, Oct 31, '13
 * @author         Enter your name here...
 */
public class NetcdfRecord extends DataRecord {

    /** _more_ */
    private PointFeatureIterator iterator;

    /** _more_ */
    private List<RecordField> dataFields = new ArrayList<RecordField>();


    /**
     * _more_
     *
     * @param file _more_
     * @param fields _more_
     * @param iterator _more_
     */
    public NetcdfRecord(RecordFile file, List<RecordField> fields,
                        PointFeatureIterator iterator) {
        super(file, fields);
        this.iterator = iterator;
        initFields(fields);
        for (int i = 3; i < fields.size(); i++) {
            dataFields.add(fields.get(i));
        }
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
    public ReadStatus read(RecordIO recordIO) throws IOException {



        if ( !iterator.hasNext()) {
            return ReadStatus.EOF;
        }



        PointFeature                      po = (PointFeature) iterator.next();
        StructureData                     structure = po.getData();
        ucar.unidata.geoloc.EarthLocation el        = po.getLocation();
        if (el == null) {
            System.err.println("skipping");

            return ReadStatus.SKIP;
        }
        setLocation(el.getLongitude(), el.getLatitude(), el.getAltitude());
        int cnt = 0;
        //        System.err.println("Latitude:" + el.getLatitude());
        values[cnt++] = el.getLatitude();
        values[cnt++] = el.getLongitude();

        //        Date dttm = po.getNominalTimeAsDate();
        CalendarDate cdttm = po.getNominalTimeAsCalendarDate();
        Date         dttm  = new Date(cdttm.getMillis());

        objectValues[cnt++] = dttm;
        setRecordTime(dttm.getTime());


        //TODO: Time
        //        System.err.println ("reading:" +el);

        for (RecordField field : dataFields) {
            StructureMembers.Member member =
                structure.findMember(field.getName());
            if (field.isTypeString()) {
                objectValues[cnt] = structure.getScalarString(member);
            } else {
                values[cnt] = structure.convertScalarFloat(member);
                //                System.err.println ("reading:" +values[cnt]);
            }
            cnt++;
        }


        return ReadStatus.OK;
    }


}
