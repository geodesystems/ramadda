/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.data.point.icebridge;


import org.ramadda.data.point.PointFile;

import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;



/** This is generated code from generate.tcl. Do not edit it! */
public class ParisFile extends org.ramadda.data.point.text.TextFile {

    /**
     * _more_
     */
    public ParisFile() {}

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws java.io.IOException _more_
     */
    public ParisFile(String filename) throws java.io.IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public Record doMakeRecord(VisitInfo visitInfo) {
        return new ParisRecord(this);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, ParisFile.class);
    }


    //generated record class


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Feb 28, '15
     * @author         Enter your name here...
     */
    public static class ParisRecord extends org.ramadda.data.point.text
        .TextRecord {

        /** _more_ */
        public static final int ATTR_FIRST =
            org.ramadda.data.point.PointRecord.ATTR_LAST;

        /** _more_ */
        public static final List<RecordField> FIELDS =
            new ArrayList<RecordField>();

        /** _more_ */
        public static final int ATTR_LAT = ATTR_FIRST + 1;

        /** _more_ */
        public static final RecordField RECORDATTR_LAT;

        /** _more_ */
        public static final int ATTR_LON = ATTR_FIRST + 2;

        /** _more_ */
        public static final RecordField RECORDATTR_LON;

        /** _more_ */
        public static final int ATTR_TIME = ATTR_FIRST + 3;

        /** _more_ */
        public static final RecordField RECORDATTR_TIME;

        /** _more_ */
        public static final int ATTR_THICKNESS = ATTR_FIRST + 4;

        /** _more_ */
        public static final RecordField RECORDATTR_THICKNESS;

        /** _more_ */
        public static final int ATTR_AIRCRAFTALTITUDE = ATTR_FIRST + 5;

        /** _more_ */
        public static final RecordField RECORDATTR_AIRCRAFTALTITUDE;

        /** _more_ */
        public static final int ATTR_CONFIDENCE = ATTR_FIRST + 6;

        /** _more_ */
        public static final RecordField RECORDATTR_CONFIDENCE;

        /** _more_ */
        public static final int ATTR_LAST = ATTR_FIRST + 7;


        static {
            FIELDS.add(RECORDATTR_LAT = new RecordField("lat", "lat", "",
                    ATTR_LAT, "", "double", "double", 0, SEARCHABLE_NO,
                    CHARTABLE_NO));
            RECORDATTR_LAT.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((ParisRecord) record).lat;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((ParisRecord) record).lat;
                }
            });
            FIELDS.add(RECORDATTR_LON = new RecordField("lon", "lon", "",
                    ATTR_LON, "", "double", "double", 0, SEARCHABLE_NO,
                    CHARTABLE_NO));
            RECORDATTR_LON.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((ParisRecord) record).lon;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((ParisRecord) record).lon;
                }
            });
            FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "",
                    ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,
                    CHARTABLE_NO));
            RECORDATTR_TIME.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((ParisRecord) record).time;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((ParisRecord) record).time;
                }
            });
            FIELDS.add(RECORDATTR_THICKNESS = new RecordField("thickness",
                    "thickness", "", ATTR_THICKNESS, "", "double", "double",
                    0, SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_THICKNESS.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((ParisRecord) record).thickness;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((ParisRecord) record).thickness;
                }
            });
            FIELDS.add(RECORDATTR_AIRCRAFTALTITUDE =
                new RecordField("aircraftAltitude", "aircraftAltitude", "",
                                ATTR_AIRCRAFTALTITUDE, "", "double",
                                "double", 0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_AIRCRAFTALTITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((ParisRecord) record).aircraftAltitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((ParisRecord) record).aircraftAltitude;
                }
            });
            FIELDS.add(RECORDATTR_CONFIDENCE = new RecordField("confidence",
                    "confidence", "", ATTR_CONFIDENCE, "", "int", "int", 0,
                    SEARCHABLE_YES, CHARTABLE_YES));
            RECORDATTR_CONFIDENCE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((ParisRecord) record).confidence;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((ParisRecord) record).confidence;
                }
            });

        }


        /** _more_ */
        double lat;

        /** _more_ */
        double lon;

        /** _more_ */
        double time;

        /** _more_ */
        double thickness;

        /** _more_ */
        double aircraftAltitude;

        /** _more_ */
        int confidence;


        /**
         * _more_
         *
         * @param that _more_
         */
        public ParisRecord(ParisRecord that) {
            super(that);
            this.lat              = that.lat;
            this.lon              = that.lon;
            this.time             = that.time;
            this.thickness        = that.thickness;
            this.aircraftAltitude = that.aircraftAltitude;
            this.confidence       = that.confidence;


        }



        /**
         * _more_
         *
         * @param file _more_
         */
        public ParisRecord(RecordFile file) {
            super(file, FIELDS);
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getLastAttribute() {
            return ATTR_LAST;
        }



        /**
         * _more_
         *
         * @param object _more_
         *
         * @return _more_
         */
        public boolean equals(Object object) {
            if ( !super.equals(object)) {
                System.err.println("bad super");

                return false;
            }
            if ( !(object instanceof ParisRecord)) {
                return false;
            }
            ParisRecord that = (ParisRecord) object;
            if (this.lat != that.lat) {
                System.err.println("bad lat");

                return false;
            }
            if (this.lon != that.lon) {
                System.err.println("bad lon");

                return false;
            }
            if (this.time != that.time) {
                System.err.println("bad time");

                return false;
            }
            if (this.thickness != that.thickness) {
                System.err.println("bad thickness");

                return false;
            }
            if (this.aircraftAltitude != that.aircraftAltitude) {
                System.err.println("bad aircraftAltitude");

                return false;
            }
            if (this.confidence != that.confidence) {
                System.err.println("bad confidence");

                return false;
            }

            return true;
        }




        /**
         * _more_
         *
         * @return _more_
         */
        public double getLatitude() {
            return lat;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLongitude() {
            return lon;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getAltitude() {
            return aircraftAltitude;
        }


        /**
         * _more_
         *
         * @param fields _more_
         */
        @Override
        protected void addFields(List<RecordField> fields) {
            super.addFields(fields);
            fields.addAll(FIELDS);
        }



        /**
         * _more_
         *
         * @param attrId _more_
         *
         * @return _more_
         */
        public double getValue(int attrId) {
            if (attrId == ATTR_LAT) {
                return lat;
            }
            if (attrId == ATTR_LON) {
                return lon;
            }
            if (attrId == ATTR_TIME) {
                return time;
            }
            if (attrId == ATTR_THICKNESS) {
                return thickness;
            }
            if (attrId == ATTR_AIRCRAFTALTITUDE) {
                return aircraftAltitude;
            }
            if (attrId == ATTR_CONFIDENCE) {
                return confidence;
            }

            return super.getValue(attrId);

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getRecordSize() {
            return super.getRecordSize() + 44;
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
        public ReadStatus read(RecordIO recordIO) throws Exception {
            ReadStatus status = ReadStatus.OK;
            String     line   = recordIO.readLine();
            if (line == null) {
                return ReadStatus.EOF;
            }
            line = line.trim();
            if (line.length() == 0) {
                return status;
            }
            String[] toks     = line.split(" +");
            int      fieldCnt = 0;
            lat              = (double) Double.parseDouble(toks[fieldCnt++]);
            lon              = (double) Double.parseDouble(toks[fieldCnt++]);
            time             = (double) Double.parseDouble(toks[fieldCnt++]);
            thickness        = (double) Double.parseDouble(toks[fieldCnt++]);
            aircraftAltitude = (double) Double.parseDouble(toks[fieldCnt++]);
            confidence       = (int) Double.parseDouble(toks[fieldCnt++]);


            return status;
        }



        /**
         * _more_
         *
         * @param recordIO _more_
         *
         * @throws IOException _more_
         */
        public void write(RecordIO recordIO) throws IOException {
            String      delimiter   = " ";
            PrintWriter printWriter = recordIO.getPrintWriter();
            printWriter.print(lat);
            printWriter.print(delimiter);
            printWriter.print(lon);
            printWriter.print(delimiter);
            printWriter.print(time);
            printWriter.print(delimiter);
            printWriter.print(thickness);
            printWriter.print(delimiter);
            printWriter.print(aircraftAltitude);
            printWriter.print(delimiter);
            printWriter.print(confidence);
            printWriter.print("\n");

        }



        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param pw _more_
         *
         * @return _more_
         */
        public int doPrintCsv(VisitInfo visitInfo, PrintWriter pw) {
            boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                        false);
            int superCnt = super.doPrintCsv(visitInfo, pw);
            int myCnt    = 0;
            if (superCnt > 0) {
                pw.print(',');
            }
            pw.print(lat);
            myCnt++;
            pw.print(',');
            pw.print(lon);
            myCnt++;
            pw.print(',');
            pw.print(time);
            myCnt++;
            pw.print(',');
            pw.print(thickness);
            myCnt++;
            pw.print(',');
            pw.print(aircraftAltitude);
            myCnt++;
            pw.print(',');
            pw.print(confidence);
            myCnt++;

            return myCnt + superCnt;

        }



        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param pw _more_
         *
         * @return _more_
         */
        public int doPrintCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
            int superCnt = super.doPrintCsvHeader(visitInfo, pw);
            int myCnt    = 0;
            boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                        false);
            if (superCnt > 0) {
                pw.print(',');
            }
            RECORDATTR_LAT.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_LON.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_TIME.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_THICKNESS.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_AIRCRAFTALTITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_CONFIDENCE.printCsvHeader(visitInfo, pw);
            myCnt++;

            return myCnt + superCnt;

        }



        /**
         * _more_
         *
         * @param buff _more_
         *
         * @throws Exception _more_
         */
        public void print(Appendable buff) throws Exception {
            super.print(buff);
            buff.append(" lat: " + lat + " \n");
            buff.append(" lon: " + lon + " \n");
            buff.append(" time: " + time + " \n");
            buff.append(" thickness: " + thickness + " \n");
            buff.append(" aircraftAltitude: " + aircraftAltitude + " \n");
            buff.append(" confidence: " + confidence + " \n");

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public double getLat() {
            return lat;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setLat(double newValue) {
            lat = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getLon() {
            return lon;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setLon(double newValue) {
            lon = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getTime() {
            return time;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setTime(double newValue) {
            time = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getThickness() {
            return thickness;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setThickness(double newValue) {
            thickness = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getAircraftAltitude() {
            return aircraftAltitude;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setAircraftAltitude(double newValue) {
            aircraftAltitude = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getConfidence() {
            return confidence;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setConfidence(int newValue) {
            confidence = newValue;
        }



    }

}
