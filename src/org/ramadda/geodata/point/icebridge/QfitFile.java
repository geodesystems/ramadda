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

package org.ramadda.geodata.point.icebridge;


import org.ramadda.util.IO;
import org.ramadda.data.point.PointFile;
import org.ramadda.data.record.*;

import org.ramadda.repository.RepositoryUtil;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.Date;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class QfitFile extends PointFile {

    /** _more_ */
    public static final int TYPE_UNDEFINED = 0;

    /** _more_ */
    public static final int TYPE_10WORD = 10;

    /** _more_ */
    public static final int TYPE_12WORD = 12;

    /** _more_ */
    public static final int TYPE_14WORD = 14;

    //Some files are little endian, some are big

    /** _more_ */
    private boolean bigEndian = true;

    /** _more_ */
    private int type = TYPE_UNDEFINED;

    //We extract this from the file name if possible

    /** _more_ */
    private long baseDate = 0L;

    /**
     * _more_
     */
    public QfitFile() {}

    /**
     * _more_
     *
     *
     * @throws java.io.IOException _more_
     */
    public QfitFile(IO.Path path) throws java.io.IOException {
        super(path);
    }

    /**
     * This gets called before the file is visited. It determines the base date from the file name,
     * reads the record size values, figures out if the file is little endian and then reads through the
     * header.
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //Get the date from the filename
        String dttm =
            StringUtil.findPattern(
                getFilename(),
                ".*(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d\\d\\d).*");
        if (dttm != null) {
            try {
                Date date = RepositoryUtil.makeDateFormat(
                                "yyyyMMdd_HHmmss").parse(dttm);
                baseDate = date.getTime();
            } catch (Exception exc) {
                System.err.println("error:" + exc);
            }
        }

        DataInputStream dis = visitInfo.getRecordIO().getDataInputStream();
        //Check the type
        int recordSize = dis.readInt();
        type = recordSize / 4;

        //If its unknown then see if its little endian
        if ((type != TYPE_10WORD) && (type != TYPE_12WORD)
                && (type != TYPE_14WORD)) {
            visitInfo.getRecordIO().close();
            visitInfo.setRecordIO(doMakeInputIO(visitInfo,getSkip(visitInfo) == 0));
            dis        = visitInfo.getRecordIO().getDataInputStream();
            bigEndian  = false;
            recordSize = readInt(dis);
            type       = recordSize / 4;
            if ((type != TYPE_10WORD) && (type != TYPE_12WORD)
                    && (type != TYPE_14WORD)) {
                throw new IllegalArgumentException("Unknown record size:"
                        + type + " Endian problem?");
            }
        }


        //Now read the rest of the header and count the header blocks
        int    numHeaderRecords = 0;
        byte[] header           = new byte[recordSize - 4];
        dis.read(header);
        numHeaderRecords++;
        StringBuffer sb = new StringBuffer();
        while (true) {
            int size = readInt(dis);
            if (size >= 0) {
                dis.read(header);

                break;
            }
            dis.read(header);
            sb.append(new String(header));
            numHeaderRecords++;
        }

        //reset and read the header records
        visitInfo.setRecordIO(doMakeInputIO(visitInfo,getSkip(visitInfo) == 0));
        dis = visitInfo.getRecordIO().getDataInputStream();
        for (int i = 0; i < numHeaderRecords; i++) {
            int size = readInt(dis);
            dis.read(header);
        }

        return visitInfo;
    }


    /**
     * Create the record.
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public BaseRecord doMakeRecord(VisitInfo visitInfo) {
        //If we haven't determined the type then explicitly call prepareToVisit
        if (type == TYPE_UNDEFINED) {
            try {
		RecordIO io = doMakeInputIO(visitInfo);
		visitInfo.setRecordIO(io);
                prepareToVisit(visitInfo);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        //Make the appropriate record, set its base date and return it
        QfitRecord record;
        if (type == TYPE_10WORD) {
            record = new QFit10WordRecord(this, bigEndian);
        } else if (type == TYPE_12WORD) {
            record = new QFit12WordRecord(this, bigEndian);
        } else if (type == TYPE_14WORD) {
            record = new QFit14WordRecord(this, bigEndian);
        } else {
            throw new IllegalArgumentException("Unknown type:" + type);
        }
        record.setBaseDate(baseDate);

        return record;
    }


    /**
     * _more_
     *
     * @param dis _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int readInt(DataInputStream dis) throws IOException {
        if (bigEndian) {
            return dis.readInt();
        }

        return readLEInt(dis);
    }

    /**
     * _more_
     *
     * @param dis _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public int readLEInt(DataInputStream dis) throws IOException {
        int accum = 0;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= (dis.readByte() & 0xff) << shiftBy;
        }

        return accum;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, QfitFile.class);
    }


}
